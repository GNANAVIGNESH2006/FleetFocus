package com.example.fleetfocus.service;

import com.example.fleetfocus.dto.DashboardDto;
import com.example.fleetfocus.entity.Driver;
import com.example.fleetfocus.entity.DriverStatus;
import com.example.fleetfocus.entity.Trip;
import com.example.fleetfocus.entity.TripStatus;
import com.example.fleetfocus.entity.VehicleStatus;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.SystemUserRepository;
import com.example.fleetfocus.repository.TripRepository;
import com.example.fleetfocus.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final SystemUserRepository systemUserRepository;

    @Value("${app.trip.overdue-hours:8}")
    private long overdueHours;

    public DashboardService(VehicleRepository vehicleRepository,
                            DriverRepository driverRepository,
                            TripRepository tripRepository,
                            SystemUserRepository systemUserRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
        this.systemUserRepository = systemUserRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboardStats(String username, String role) {
        DashboardDto dto = new DashboardDto();
        dto.setRole(role);

        switch (role) {
            case "ADMIN" -> populateAdmin(dto);
            case "DISPATCHER" -> populateDispatcher(dto);
            case "DRIVER" -> populateDriver(dto, username);
            default -> { }
        }

        return dto;
    }

    private void populateAdmin(DashboardDto dto) {
        long total = vehicleRepository.count();
        long available = vehicleRepository.countByStatus(VehicleStatus.AVAILABLE);
        long onTrip = vehicleRepository.countByStatus(VehicleStatus.ON_TRIP);
        long underMaintenance = vehicleRepository.countByStatus(VehicleStatus.UNDER_MAINTENANCE);

        dto.setTotalVehicles(total);
        dto.setAvailableVehicles(available);
        dto.setVehiclesOnTrip(onTrip);
        dto.setVehiclesUnderMaintenance(underMaintenance);

        dto.setTotalDrivers(driverRepository.count());
        dto.setAvailableDrivers(driverRepository.countByStatus(DriverStatus.AVAILABLE));

        dto.setTotalUsers(systemUserRepository.count());

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LocalDateTime overdueThreshold = LocalDateTime.now().minusHours(overdueHours);
        long overdue = tripRepository.countByStatusAndStartTimeBefore(TripStatus.ACTIVE, overdueThreshold);

        dto.setTotalTrips(tripRepository.count());
        dto.setActiveTrips(tripRepository.countByStatus(TripStatus.ACTIVE));
        dto.setCompletedTrips(tripRepository.countByStatus(TripStatus.COMPLETED));
        dto.setTodaysTrips(tripRepository.countByStartTimeBetween(startOfDay, endOfDay));
        dto.setOverdueTrips(overdue);
        dto.setPendingAssignments(overdue);
    }

    private void populateDispatcher(DashboardDto dto) {
        long availableVehicles = vehicleRepository.countByStatus(VehicleStatus.AVAILABLE);
        long availableDrivers = driverRepository.countByStatus(DriverStatus.AVAILABLE);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LocalDateTime overdueThreshold = LocalDateTime.now().minusHours(overdueHours);
        long overdue = tripRepository.countByStatusAndStartTimeBefore(TripStatus.ACTIVE, overdueThreshold);

        dto.setAvailableVehicles(availableVehicles);
        dto.setAvailableDrivers(availableDrivers);
        dto.setTodaysTrips(tripRepository.countByStartTimeBetween(startOfDay, endOfDay));
        dto.setActiveTrips(tripRepository.countByStatus(TripStatus.ACTIVE));
        dto.setCompletedTrips(tripRepository.countByStatus(TripStatus.COMPLETED));
        dto.setOverdueTrips(overdue);
        dto.setPendingAssignments(overdue);
    }

    private void populateDriver(DashboardDto dto, String username) {
        Optional<Driver> driverOpt = driverRepository.findByUsername(username);
        if (driverOpt.isEmpty()) {
            return;
        }
        Driver driver = driverOpt.get();
        dto.setDriverName(driver.getName());

        List<Trip> activeTrips = tripRepository.findByDriver_UsernameAndStatus(username, TripStatus.ACTIVE);
        if (!activeTrips.isEmpty()) {
            Trip currentTrip = activeTrips.get(0);
            dto.setMyCurrentTripId(currentTrip.getId());
            dto.setMyCurrentTripStatus(currentTrip.getStatus().name());
            dto.setMyVehiclePlate(currentTrip.getVehicle().getLicensePlate());
            dto.setMyVehicleModel(currentTrip.getVehicle().getModel());
            dto.setMyVehicleStatus(currentTrip.getVehicle().getStatus().name());
        }

        long completed = tripRepository.countByDriver_UsernameAndStatus(username, TripStatus.COMPLETED);
        dto.setMyCompletedTripsCount(completed);
    }
}
