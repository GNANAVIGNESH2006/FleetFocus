package com.example.fleetfocus;

import com.example.fleetfocus.dto.MaintenanceRequestDto;
import com.example.fleetfocus.dto.TripResponseDto;
import com.example.fleetfocus.entity.*;
import com.example.fleetfocus.exception.ConflictException;
import com.example.fleetfocus.repository.*;
import com.example.fleetfocus.service.MaintenanceService;
import com.example.fleetfocus.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TripServiceIntegrationTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MaintenanceLogRepository maintenanceLogRepository;

    private Vehicle vehicle;
    private Driver driver;
    private Authentication adminAuth;
    private Authentication driverAuth;

    @BeforeEach
    void setUp() {
        maintenanceLogRepository.deleteAll();
        tripRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();

        vehicle = vehicleRepository.save(new Vehicle(
                null, "1FTFW1ET5DFC10312", "FF-2001", "Volvo FH16", VehicleStatus.AVAILABLE, 1000.0));
        driver = driverRepository.save(new Driver(
                null, "Alex Driver", "DL-90001", DriverStatus.AVAILABLE, "alexdriver"));

        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        driverAuth = new UsernamePasswordAuthenticationToken(
                "alexdriver", null, List.of(new SimpleGrantedAuthority("ROLE_DRIVER")));
    }

    @Test
    void startAndEndTripUpdatesStatusesAndMileage() {
        TripResponseDto started = tripService.startTrip(vehicle.getId(), driver.getId(), driverAuth);
        assertEquals("ACTIVE", started.getStatus());

        assertEquals(VehicleStatus.ON_TRIP, vehicleRepository.findById(vehicle.getId()).orElseThrow().getStatus());
        assertEquals(DriverStatus.ON_TRIP, driverRepository.findById(driver.getId()).orElseThrow().getStatus());

        // Starting another trip with same vehicle or driver must throw ConflictException (409)
        assertThrows(ConflictException.class,
                () -> tripService.startTrip(vehicle.getId(), driver.getId(), adminAuth));

        TripResponseDto ended = tripService.endTrip(started.getId(), 150.5, driverAuth);
        assertEquals("COMPLETED", ended.getStatus());
        assertEquals(150.5, ended.getDistanceCovered());

        Vehicle updatedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(VehicleStatus.AVAILABLE, updatedVehicle.getStatus());
        assertEquals(1150.5, updatedVehicle.getCurrentMileage(), 0.001);

        Driver updatedDriver = driverRepository.findById(driver.getId()).orElseThrow();
        assertEquals(DriverStatus.AVAILABLE, updatedDriver.getStatus());

        // Ending an already completed trip must throw ConflictException (409)
        assertThrows(ConflictException.class,
                () -> tripService.endTrip(started.getId(), 50.0, adminAuth));
    }

    @Test
    void driverWithUnlinkedUsernameOrOtherDriverGetsAccessDeniedNotNpe() {
        Driver unlinkedDriver = driverRepository.save(new Driver(
                null, "Unlinked Bob", "DL-90002", DriverStatus.AVAILABLE, null));

        // Driver trying to start trip for unlinked driver -> AccessDeniedException (403), not NullPointerException
        assertThrows(AccessDeniedException.class,
                () -> tripService.startTrip(vehicle.getId(), unlinkedDriver.getId(), driverAuth));
    }

    @Test
    void endingTripWithOpenMaintenanceTransitionsVehicleToUnderMaintenance() {
        TripResponseDto started = tripService.startTrip(vehicle.getId(), driver.getId(), driverAuth);

        // Driver reports an issue during active trip -> vehicle stays ON_TRIP while trip is active
        MaintenanceRequestDto req = new MaintenanceRequestDto(
                LocalDate.now(), "Brake pad warning light", new BigDecimal("2500.00"));
        maintenanceService.logMaintenance(vehicle.getId(), req, driverAuth);

        assertEquals(VehicleStatus.ON_TRIP, vehicleRepository.findById(vehicle.getId()).orElseThrow().getStatus());

        // When trip ends, vehicle transitions to UNDER_MAINTENANCE because open maintenance exists
        tripService.endTrip(started.getId(), 80.0, driverAuth);
        assertEquals(VehicleStatus.UNDER_MAINTENANCE,
                vehicleRepository.findById(vehicle.getId()).orElseThrow().getStatus());
    }
}
