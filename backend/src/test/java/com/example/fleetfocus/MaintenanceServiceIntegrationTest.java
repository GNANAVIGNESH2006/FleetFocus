package com.example.fleetfocus;

import com.example.fleetfocus.dto.MaintenanceRequestDto;
import com.example.fleetfocus.dto.MaintenanceResponseDto;
import com.example.fleetfocus.entity.Vehicle;
import com.example.fleetfocus.entity.VehicleStatus;
import com.example.fleetfocus.exception.ConflictException;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.MaintenanceLogRepository;
import com.example.fleetfocus.repository.TripRepository;
import com.example.fleetfocus.repository.VehicleRepository;
import com.example.fleetfocus.service.MaintenanceService;
import com.example.fleetfocus.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MaintenanceServiceIntegrationTest {

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MaintenanceLogRepository maintenanceLogRepository;

    private Vehicle vehicle;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        maintenanceLogRepository.deleteAll();
        tripRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();

        vehicle = vehicleRepository.save(new Vehicle(
                null, "1FTFW1ET5DFC10312", "FF-3001", "Mercedes Sprinter", VehicleStatus.AVAILABLE, 5000.0));
        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void maintenanceLifecycleTransitionsVehicleStatusCorrectly() {
        MaintenanceRequestDto req1 = new MaintenanceRequestDto(
                LocalDate.now(), "Oil change and filter replacement", new BigDecimal("1200.50"));
        MaintenanceResponseDto log1 = maintenanceService.logMaintenance(vehicle.getId(), req1, adminAuth);

        assertEquals("IN_PROGRESS", log1.getStatus());
        assertNull(log1.getCompletedDate());
        assertEquals(VehicleStatus.UNDER_MAINTENANCE,
                vehicleRepository.findById(vehicle.getId()).orElseThrow().getStatus());

        // Cannot manually set vehicle to AVAILABLE while open maintenance exists
        assertThrows(ConflictException.class,
                () -> vehicleService.updateVehicleStatus(vehicle.getId(), VehicleStatus.AVAILABLE));

        // Add a second open maintenance log on the same vehicle
        MaintenanceRequestDto req2 = new MaintenanceRequestDto(
                LocalDate.now(), "Tire alignment", new BigDecimal("800.00"));
        MaintenanceResponseDto log2 = maintenanceService.logMaintenance(vehicle.getId(), req2, adminAuth);

        // Complete first log -> vehicle remains UNDER_MAINTENANCE because log2 is still IN_PROGRESS
        MaintenanceResponseDto completed1 = maintenanceService.completeMaintenance(log1.getId());
        assertEquals("COMPLETED", completed1.getStatus());
        assertNotNull(completed1.getCompletedDate());
        assertEquals(VehicleStatus.UNDER_MAINTENANCE,
                vehicleRepository.findById(vehicle.getId()).orElseThrow().getStatus());

        // Completing already completed log -> ConflictException (409)
        assertThrows(ConflictException.class, () -> maintenanceService.completeMaintenance(log1.getId()));

        // Complete second log -> vehicle returns to AVAILABLE
        MaintenanceResponseDto completed2 = maintenanceService.completeMaintenance(log2.getId());
        assertEquals("COMPLETED", completed2.getStatus());
        assertEquals(VehicleStatus.AVAILABLE,
                vehicleRepository.findById(vehicle.getId()).orElseThrow().getStatus());
    }

    @Test
    void deletingOpenMaintenanceRecalculatesVehicleStatusToAvailable() {
        MaintenanceRequestDto req = new MaintenanceRequestDto(
                LocalDate.now(), "Battery check", new BigDecimal("300.00"));
        MaintenanceResponseDto created = maintenanceService.logMaintenance(vehicle.getId(), req, adminAuth);

        assertEquals(VehicleStatus.UNDER_MAINTENANCE,
                vehicleRepository.findById(vehicle.getId()).orElseThrow().getStatus());

        maintenanceService.deleteMaintenance(created.getId());

        assertEquals(VehicleStatus.AVAILABLE,
                vehicleRepository.findById(vehicle.getId()).orElseThrow().getStatus());
    }
}
