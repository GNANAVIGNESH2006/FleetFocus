package com.example.fleetfocus.service;

import com.example.fleetfocus.entity.Driver;
import com.example.fleetfocus.entity.MaintenanceLog;
import com.example.fleetfocus.entity.Trip;
import com.example.fleetfocus.entity.TripStatus;
import com.example.fleetfocus.entity.Vehicle;
import com.example.fleetfocus.entity.VehicleStatus;
import com.example.fleetfocus.exception.ResourceNotFoundException;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.MaintenanceLogRepository;
import com.example.fleetfocus.repository.TripRepository;
import com.example.fleetfocus.repository.VehicleRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaintenanceService {

    private final MaintenanceLogRepository maintenanceLogRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;

    public MaintenanceService(MaintenanceLogRepository maintenanceLogRepository,
                              VehicleRepository vehicleRepository,
                              DriverRepository driverRepository,
                              TripRepository tripRepository) {
        this.maintenanceLogRepository = maintenanceLogRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
    }

    public List<MaintenanceLog> getAllLogs() {
        return maintenanceLogRepository.findAll();
    }

    public MaintenanceLog logMaintenance(Long vehicleId, MaintenanceLog log, Authentication authentication) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id: " + vehicleId));

        if (isDriver(authentication)) {
            Driver driver = driverRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No driver profile is linked to this account"));

            List<Trip> activeTrips = tripRepository.findByDriver_UsernameAndStatus(
                    authentication.getName(), TripStatus.ACTIVE);

            boolean ownsVehicle = activeTrips.stream()
                    .anyMatch(t -> t.getVehicle().getId().equals(vehicleId));

            if (!ownsVehicle) {
                throw new AccessDeniedException(
                        "Driver '" + driver.getName() + "' can only report issues for their own assigned vehicle");
            }
        }

        vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
        vehicleRepository.save(vehicle);

        log.setVehicle(vehicle);

        return maintenanceLogRepository.save(log);
    }

    public void deleteMaintenance(Long id) {

        MaintenanceLog log = maintenanceLogRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Maintenance log not found with id: " + id));

        maintenanceLogRepository.delete(log);
    }

    private boolean isDriver(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_DRIVER"));
    }
}

