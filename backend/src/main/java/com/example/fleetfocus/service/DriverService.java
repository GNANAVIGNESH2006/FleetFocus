package com.example.fleetfocus.service;

import com.example.fleetfocus.entity.Driver;
import com.example.fleetfocus.entity.DriverStatus;
import com.example.fleetfocus.exception.ResourceNotFoundException;
import com.example.fleetfocus.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public List<Driver> getAvailableDrivers() {
        return driverRepository.findByStatus(DriverStatus.AVAILABLE);
    }

    public Driver getDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
    }

    public Driver getDriverByUsername(String username) {
        return driverRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No driver profile is linked to this account"));
    }

    public Driver createDriver(Driver driver) {
        if (driver.getStatus() == null) {
            driver.setStatus(DriverStatus.AVAILABLE);
        }
        return driverRepository.save(driver);
    }

    public Driver updateDriver(Long id, Driver updatedDriver) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver not found with id: " + id));

        driver.setName(updatedDriver.getName());
        driver.setLicenseNumber(updatedDriver.getLicenseNumber());
        driver.setStatus(updatedDriver.getStatus());
        driver.setUsername(updatedDriver.getUsername());

        return driverRepository.save(driver);
    }

    public Driver updateDriverStatus(Long id, String status) {
        Driver driver = getDriverById(id);
        try {
            driver.setStatus(DriverStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid driver status: " + status);
        }
        return driverRepository.save(driver);
    }

    public void deleteDriver(Long id) {
        Driver driver = getDriverById(id);
        driverRepository.delete(driver);
    }
}

