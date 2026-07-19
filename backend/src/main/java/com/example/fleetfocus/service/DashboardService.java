package com.example.fleetfocus.service;

import com.example.fleetfocus.dto.DashboardDto;
import com.example.fleetfocus.entity.Driver;
import com.example.fleetfocus.entity.Trip;
import com.example.fleetfocus.entity.TripStatus;
import com.example.fleetfocus.entity.VehicleStatus;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.SystemUserRepository;
import com.example.fleetfocus.repository.TripRepository;
import com.example.fleetfocus.repository.VehicleRepository;
import org.springframework.stereotype.Service;

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

    public DashboardService(VehicleRepository vehicleRepository,
                             DriverRepository driverRepository,
                             TripRepository tripRepository,
                             SystemUserRepository systemUserRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
        this.systemUserRepository = systemUserRepository;
    }

    public DashboardDto getDashboardStats(String username, String role) {
        DashboardDto dto = new DashboardDto();
        dto.setRole(role);

        switch (role) {
            case "ADMIN" -> populateAdmin(dto);
            case "DISPATCHER" -> populateDispatcher(dto);
            case "DRIVER" -> populateDriver(dto, username);
            default -> {  }
        }

        return dto;
    }

    private void populateAdmin(DashboardDto dto) {
        long total = vehicleRepository.count();
        long available = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).size();
        long onTrip = vehicleRepository.findByStatus(VehicleStatus.ON_TRIP).size();
        long underMaintenance = vehicleRepository.findByStatus(VehicleStatus.UNDER_MAINTENANCE).size();

        dto.setTotalVehicles(total);
        dto.setAvailableVehicles(available);
        dto.setVehiclesOnTrip(onTrip);
        dto.setVehiclesUnderMaintenance(underMaintenance);

        dto.setTotalDrivers((long) driverRepository.findAll().size());
        dto.setAvailableDrivers((long) driverRepository.findByStatus(
                com.example.fleetfocus.entity.DriverStatus.AVAILABLE).size());

        dto.setTotalUsers(systemUserRepository.count());

        dto.setTotalTrips(tripRepository.count());
        dto.setCompletedTrips(tripRepository.countByStatus(TripStatus.COMPLETED));
    }

    private void populateDispatcher(DashboardDto dto) {
        long availableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).size();
        long availableDrivers = driverRepository.findByStatus(
                com.example.fleetfocus.entity.DriverStatus.AVAILABLE).size();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        dto.setAvailableVehicles(availableVehicles);
        dto.setAvailableDrivers(availableDrivers);
        dto.setTodaysTrips(tripRepository.countByStartTimeBetween(startOfDay, endOfDay));
        dto.setActiveTrips(tripRepository.countByStatus(TripStatus.ACTIVE));

        dto.setPendingAssignments(Math.min(availableVehicles, availableDrivers));
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

        long completed = tripRepository.findByDriver_UsernameAndStatus(username, TripStatus.COMPLETED).size();
        dto.setMyCompletedTripsCount(completed);
    }
}

