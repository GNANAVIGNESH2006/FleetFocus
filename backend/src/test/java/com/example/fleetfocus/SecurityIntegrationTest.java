package com.example.fleetfocus;

import com.example.fleetfocus.entity.SystemUser;
import com.example.fleetfocus.entity.UserRole;
import com.example.fleetfocus.entity.Vehicle;
import com.example.fleetfocus.entity.VehicleStatus;
import com.example.fleetfocus.repository.*;
import com.example.fleetfocus.security.JwtService;
import com.example.fleetfocus.security.LoginAttemptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MaintenanceLogRepository maintenanceLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService.clearAll();
        maintenanceLogRepository.deleteAll();
        tripRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();
        systemUserRepository.deleteAll();
    }

    @Test
    void unauthenticatedAccessReturns401Json() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required or token expired"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void publicRegistrationAlwaysCreatesDriverAndRejectsDuplicatesWith409() throws Exception {
        String payload = """
                {
                  "username": "  newdriver  ",
                  "email": "newdriver@fleetfocus.com",
                  "password": "password123",
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        SystemUser created = systemUserRepository.findByUsername("newdriver").orElseThrow();
        assertEquals(UserRole.DRIVER, created.getRole());

        // Case-insensitive duplicate username -> 409
        String dupUserPayload = """
                {
                  "username": "NEWDRIVER",
                  "email": "other@fleetfocus.com",
                  "password": "password123"
                }
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dupUserPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken"));

        // Case-insensitive duplicate email -> 409
        String dupEmailPayload = """
                {
                  "username": "anotheruser",
                  "email": "NEWDRIVER@FLEETFOCUS.COM",
                  "password": "password123"
                }
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dupEmailPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void loginThrottlingLocksAfterFiveConsecutiveFailures() throws Exception {
        SystemUser user = new SystemUser(null, "throttleuser", passwordEncoder.encode("correctPass123"),
                "throttle@fleetfocus.com", UserRole.DRIVER);
        systemUserRepository.save(user);

        String badLogin = objectMapper.writeValueAsString(Map.of(
                "username", "throttleuser",
                "password", "wrongpassword"
        ));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badLogin))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        // 6th attempt -> 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLogin))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void jwtTokenInvalidatedWhenUserDeletedAndRecreatedWithSameUsername() throws Exception {
        SystemUser original = new SystemUser(null, "recreateuser", passwordEncoder.encode("password123"),
                "orig@fleetfocus.com", UserRole.ADMIN);
        original = systemUserRepository.save(original);

        String oldToken = jwtService.generateToken(original);

        // Verify token works initially
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("recreateuser"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // Delete and re-create with same username (new DB ID)
        systemUserRepository.delete(original);
        systemUserRepository.flush();

        SystemUser recreated = new SystemUser(null, "recreateuser", passwordEncoder.encode("password123"),
                "recreated@fleetfocus.com", UserRole.DRIVER);
        systemUserRepository.saveAndFlush(recreated);

        // Old token has old uid -> must be rejected with 401
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleAuthorizationAndDriverAvailableVehicleSummary() throws Exception {
        SystemUser driverUser = systemUserRepository.save(new SystemUser(
                null, "drv1", passwordEncoder.encode("password123"), "drv1@fleetfocus.com", UserRole.DRIVER));
        SystemUser adminUser = systemUserRepository.save(new SystemUser(
                null, "adm1", passwordEncoder.encode("password123"), "adm1@fleetfocus.com", UserRole.ADMIN));

        vehicleRepository.save(new Vehicle(
                null, "1FTFW1ET5DFC10312", "FF-1001", "Ford Transit", VehicleStatus.AVAILABLE, 12500.0));

        String driverToken = jwtService.generateToken(driverUser);
        String adminToken = jwtService.generateToken(adminUser);

        // DRIVER cannot access /api/users -> 403
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());

        // DRIVER calling /api/vehicles/available receives summary fields only
        mockMvc.perform(get("/api/vehicles/available")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].licensePlate").value("FF-1001"))
                .andExpect(jsonPath("$[0].model").value("Ford Transit"))
                .andExpect(jsonPath("$[0].vin").doesNotExist())
                .andExpect(jsonPath("$[0].currentMileage").doesNotExist());

        // ADMIN calling /api/vehicles/available receives full DTO
        mockMvc.perform(get("/api/vehicles/available")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vin").value("1FTFW1ET5DFC10312"))
                .andExpect(jsonPath("$[0].currentMileage").value(12500.0));
    }
}
