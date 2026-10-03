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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SystemUserRepository systemUserRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.dispatcher-password:}")
    private String dispatcherPassword;

    @Value("${app.seed.driver-password:}")
    private String driverPassword;

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
        if (adminPassword != null && !adminPassword.isBlank()
                && systemUserRepository.findByUsername("admin").isEmpty()) {
            SystemUser admin = new SystemUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setEmail("admin@fleetfocus.com");
            admin.setRole(UserRole.ADMIN);
            systemUserRepository.save(admin);
        }

        if (dispatcherPassword != null && !dispatcherPassword.isBlank()
                && systemUserRepository.findByUsername("dispatcher").isEmpty()) {
            SystemUser dispatcher = new SystemUser();
            dispatcher.setUsername("dispatcher");
            dispatcher.setPassword(passwordEncoder.encode(dispatcherPassword));
            dispatcher.setEmail("dispatcher@fleetfocus.com");
            dispatcher.setRole(UserRole.DISPATCHER);
            systemUserRepository.save(dispatcher);
        }

        if (driverPassword != null && !driverPassword.isBlank()
                && systemUserRepository.findByUsername("driver").isEmpty()) {
            SystemUser driver = new SystemUser();
            driver.setUsername("driver");
            driver.setPassword(passwordEncoder.encode(driverPassword));
            driver.setEmail("driver@fleetfocus.com");
            driver.setRole(UserRole.DRIVER);
            systemUserRepository.save(driver);

            if (driverRepository.findByUsername("driver").isEmpty()) {
                Driver driverProfile = new Driver();
                driverProfile.setName("Demo Driver");
                driverProfile.setLicenseNumber("DL-DEMO-0001");
                driverProfile.setStatus(DriverStatus.AVAILABLE);
                driverProfile.setUsername("driver");
                driverRepository.save(driverProfile);
            }
        }

        if (vehicleRepository.count() == 0) {
            Vehicle vehicle = new Vehicle();
            vehicle.setVin("1FTFW1ET5DFC10312");
            vehicle.setLicensePlate("FF-1001");
            vehicle.setModel("Ford Transit");
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicle.setCurrentMileage(0.0);
            vehicleRepository.save(vehicle);
        }

        log.info("Development data seeding check completed.");
    }
}

