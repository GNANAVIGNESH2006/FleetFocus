package com.example.fleetfocus.config;

import com.example.fleetfocus.entity.Driver;
import com.example.fleetfocus.entity.DriverStatus;
import com.example.fleetfocus.entity.SystemUser;
import com.example.fleetfocus.entity.UserRole;
import com.example.fleetfocus.entity.Vehicle;
import com.example.fleetfocus.entity.VehicleStatus;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.SystemUserRepository;
import com.example.fleetfocus.repository.VehicleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SystemUserRepository systemUserRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(SystemUserRepository systemUserRepository,
                      DriverRepository driverRepository,
                      VehicleRepository vehicleRepository,
                      PasswordEncoder passwordEncoder) {
        this.systemUserRepository = systemUserRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (systemUserRepository.findByUsername("admin").isEmpty()) {
            SystemUser admin = new SystemUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@fleetfocus.com");
            admin.setRole(UserRole.ADMIN);
            systemUserRepository.save(admin);
        }

        if (systemUserRepository.findByUsername("dispatcher").isEmpty()) {
            SystemUser dispatcher = new SystemUser();
            dispatcher.setUsername("dispatcher");
            dispatcher.setPassword(passwordEncoder.encode("dispatcher123"));
            dispatcher.setEmail("dispatcher@fleetfocus.com");
            dispatcher.setRole(UserRole.DISPATCHER);
            systemUserRepository.save(dispatcher);
        }

        if (systemUserRepository.findByUsername("driver").isEmpty()) {
            SystemUser driver = new SystemUser();
            driver.setUsername("driver");
            driver.setPassword(passwordEncoder.encode("driver123"));
            driver.setEmail("driver@fleetfocus.com");
            driver.setRole(UserRole.DRIVER);
            systemUserRepository.save(driver);
        }

        if (driverRepository.findByUsername("driver").isEmpty()) {
            Driver driverProfile = new Driver();
            driverProfile.setName("Demo Driver");
            driverProfile.setLicenseNumber("DL-DEMO-0001");
            driverProfile.setStatus(DriverStatus.AVAILABLE);
            driverProfile.setUsername("driver");
            driverRepository.save(driverProfile);
        }

        if (vehicleRepository.count() == 0) {
            Vehicle vehicle = new Vehicle();
            vehicle.setVin("DEMOVIN001");
            vehicle.setLicensePlate("FF-1001");
            vehicle.setModel("Ford Transit");
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicle.setCurrentMileage(0.0);
            vehicleRepository.save(vehicle);
        }

        System.out.println("Default users seeded successfully.");
    }
}

