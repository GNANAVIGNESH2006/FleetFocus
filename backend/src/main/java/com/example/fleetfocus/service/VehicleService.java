package com.example.fleetfocus.service;

import com.example.fleetfocus.entity.*;
import com.example.fleetfocus.exception.ResourceNotFoundException;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.TripRepository;
import com.example.fleetfocus.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                           DriverRepository driverRepository,
                           TripRepository tripRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
    }

    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
    }

    public Vehicle getVehicleForDriver(String username) {
        Driver driver = driverRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No driver profile is linked to this account"));

        List<Trip> activeTrips = tripRepository.findByDriver_UsernameAndStatus(username, TripStatus.ACTIVE);

        if (activeTrips.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Driver '" + driver.getName() + "' has no vehicle assigned right now");
        }

        return activeTrips.get(0).getVehicle();
    }

    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicle.getStatus() == null) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }
        if (vehicle.getCurrentMileage() == null) {
            vehicle.setCurrentMileage(0.0);
        }
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(Long id, Vehicle vehicleDetails) {
        Vehicle existing = getVehicleById(id);
        existing.setVin(vehicleDetails.getVin());
        existing.setLicensePlate(vehicleDetails.getLicensePlate());
        existing.setModel(vehicleDetails.getModel());
        existing.setStatus(vehicleDetails.getStatus());
        existing.setCurrentMileage(vehicleDetails.getCurrentMileage());
        return vehicleRepository.save(existing);
    }

    public Vehicle updateVehicleStatus(Long id, String status) {
        Vehicle existing = getVehicleById(id);
        try {
            existing.setStatus(VehicleStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid vehicle status: " + status);
        }
        return vehicleRepository.save(existing);
    }

    public void deleteVehicle(Long id) {
        Vehicle vehicle = getVehicleById(id);
        vehicleRepository.delete(vehicle);
    }
}

