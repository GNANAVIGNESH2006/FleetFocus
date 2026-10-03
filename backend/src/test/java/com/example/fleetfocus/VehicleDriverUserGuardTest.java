package com.example.fleetfocus;

import com.example.fleetfocus.dto.*;
import com.example.fleetfocus.entity.*;
import com.example.fleetfocus.exception.ConflictException;
import com.example.fleetfocus.repository.*;
import com.example.fleetfocus.security.JwtService;
import com.example.fleetfocus.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VehicleDriverUserGuardTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private DriverService driverService;

    @Autowired
    private TripService tripService;

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private UserService userService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MaintenanceLogRepository maintenanceLogRepository;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        maintenanceLogRepository.deleteAll();
        tripRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();
        systemUserRepository.deleteAll();

        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void vehicleAndDriverManualOnTripStatusRejectedAndSafeDeletesEnforced() {
        // Cannot create vehicle with ON_TRIP status
        VehicleRequestDto badVehicleReq = new VehicleRequestDto(
                "1FTFW1ET5DFC10312", "FF-4001", "Isuzu NPR", VehicleStatus.ON_TRIP, 0.0);
        assertThrows(ConflictException.class, () -> vehicleService.createVehicle(badVehicleReq));

        // Create valid vehicle & driver
        VehicleResponseDto v = vehicleService.createVehicle(new VehicleRequestDto(
                "1ftfw1et5dfc10312", "ff-4001", "Isuzu NPR", VehicleStatus.AVAILABLE, 100.0));
        assertEquals("1FTFW1ET5DFC10312", v.getVin());
        assertEquals("FF-4001", v.getLicensePlate());

        // Cannot manually patch vehicle status to ON_TRIP
        assertThrows(ConflictException.class,
                () -> vehicleService.updateVehicleStatus(v.getId(), VehicleStatus.ON_TRIP));

        DriverResponseDto d = driverService.createDriver(new DriverRequestDto(
                "Sam Driver", "dl-55555", DriverStatus.AVAILABLE, null));
        assertEquals("DL-55555", d.getLicenseNumber());

        // Start and complete a trip so both vehicle and driver have trip history
        TripResponseDto trip = tripService.startTrip(v.getId(), d.getId(), adminAuth);

        // Cannot manually change vehicle or driver status while ON_TRIP
        assertThrows(ConflictException.class,
                () -> vehicleService.updateVehicleStatus(v.getId(), VehicleStatus.UNDER_MAINTENANCE));
        assertThrows(ConflictException.class,
                () -> driverService.updateDriverStatus(d.getId(), DriverStatus.AVAILABLE));

        tripService.endTrip(trip.getId(), 25.0, adminAuth);

        // Safe delete (AG-16): deleting vehicle or driver with trip history must fail with ConflictException (409)
        assertThrows(ConflictException.class, () -> vehicleService.deleteVehicle(v.getId()));
        assertThrows(ConflictException.class, () -> driverService.deleteDriver(d.getId()));
    }

    @Test
    void driverAccountLinkageValidationAndUserManagementGuards() {
        UserResponseDto admin1 = userService.createUser(new CreateUserDto(
                "admin1", "admin1@fleetfocus.com", "password123", UserRole.ADMIN));
        UserResponseDto driverUser = userService.createUser(new CreateUserDto(
                "drvAccount", "drv@fleetfocus.com", "password123", UserRole.DRIVER));

        // Linking non-existent username -> IllegalArgumentException (400)
        assertThrows(IllegalArgumentException.class, () -> driverService.createDriver(
                new DriverRequestDto("Driver One", "DL-10001", DriverStatus.AVAILABLE, "nonexistent")));

        // Linking ADMIN account to driver -> IllegalArgumentException (400)
        assertThrows(IllegalArgumentException.class, () -> driverService.createDriver(
                new DriverRequestDto("Driver One", "DL-10001", DriverStatus.AVAILABLE, "admin1")));

        // Linking valid DRIVER account (case-insensitive) stores canonical username
        DriverResponseDto linkedDriver = driverService.createDriver(
                new DriverRequestDto("Driver One", "DL-10001", DriverStatus.AVAILABLE, "drvaCCount"));
        assertEquals("drvAccount", linkedDriver.getUsername());

        // Linking the same DRIVER account to a second driver -> ConflictException (409)
        assertThrows(ConflictException.class, () -> driverService.createDriver(
                new DriverRequestDto("Driver Two", "DL-10002", DriverStatus.AVAILABLE, "drvAccount")));

        // Admin cannot demote or delete self (AG-14)
        assertThrows(ConflictException.class,
                () -> userService.updateUserRole(admin1.getId(), UserRole.DISPATCHER, adminAuth));
        assertThrows(ConflictException.class,
                () -> userService.deleteUser(admin1.getId(), adminAuth));

        // Changing driverUser's role away from DRIVER unlinks Driver.username
        userService.updateUserRole(driverUser.getId(), UserRole.DISPATCHER, adminAuth);
        assertNull(driverRepository.findById(linkedDriver.getId()).orElseThrow().getUsername());
    }

    @Test
    void dtoValidationAndPaginationHeaderContract() throws Exception {
        SystemUser admin = systemUserRepository.save(new SystemUser(
                null, "admin1", passwordEncoder.encode("password123"), "adm@fleetfocus.com", UserRole.ADMIN));
        String token = jwtService.generateToken(admin);

        // Invalid VIN & negative mileage -> 400 with field errors
        String invalidVehicleJson = """
                {
                  "vin": "SHORTVIN",
                  "licensePlate": "FF-9999",
                  "model": "Test Truck",
                  "status": "AVAILABLE",
                  "currentMileage": -50.0
                }
                """;

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidVehicleJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.vin").exists())
                .andExpect(jsonPath("$.errors.currentMileage").exists());

        // Create 2 valid vehicles and verify GET /api/vehicles sets X-Total-Count header and returns JSON array
        vehicleService.createVehicle(new VehicleRequestDto(
                "1FTFW1ET5DFC10312", "FF-5001", "Truck Alpha", VehicleStatus.AVAILABLE, 100.0));
        vehicleService.createVehicle(new VehicleRequestDto(
                "2FTFW1ET5DFC10313", "FF-5002", "Truck Beta", VehicleStatus.AVAILABLE, 200.0));

        mockMvc.perform(get("/api/vehicles")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
