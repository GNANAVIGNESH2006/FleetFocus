package com.example.fleetfocus.service;

import com.example.fleetfocus.entity.*;
import com.example.fleetfocus.exception.ResourceNotFoundException;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.TripRepository;
import com.example.fleetfocus.repository.VehicleRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public TripService(TripRepository tripRepository,
                        VehicleRepository vehicleRepository,
                        DriverRepository driverRepository) {
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public List<Trip> getTripsForDriver(String username) {
        return tripRepository.findByDriver_Username(username);
    }

    public Trip startTrip(Long vehicleId, Long driverId, Authentication authentication) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        if (isDriver(authentication) && !driver.getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("Drivers can only start trips for themselves");
        }

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Vehicle is not available");
        }
        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new IllegalStateException("Driver is not available");
        }

        vehicle.setStatus(VehicleStatus.ON_TRIP);
        driver.setStatus(DriverStatus.ON_TRIP);
        vehicleRepository.save(vehicle);
        driverRepository.save(driver);

        Trip trip = new Trip();
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setStartTime(LocalDateTime.now());
        trip.setStatus(TripStatus.ACTIVE);
        trip.setDistanceCovered(0.0);

        return tripRepository.save(trip);
    }

    public Trip endTrip(Long tripId, Double distance, Authentication authentication) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        if (isDriver(authentication)
                && (trip.getDriver().getUsername() == null
                    || !trip.getDriver().getUsername().equals(authentication.getName()))) {
            throw new AccessDeniedException("Drivers can only end their own trips");
        }

        Vehicle vehicle = trip.getVehicle();
        Driver driver = trip.getDriver();

        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setCurrentMileage(vehicle.getCurrentMileage() + (distance != null ? distance : 0.0));
        driver.setStatus(DriverStatus.AVAILABLE);

        vehicleRepository.save(vehicle);
        driverRepository.save(driver);

        trip.setEndTime(LocalDateTime.now());
        trip.setDistanceCovered(distance);
        trip.setStatus(TripStatus.COMPLETED);

        return tripRepository.save(trip);
    }

    private boolean isDriver(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_DRIVER"));
    }
}

