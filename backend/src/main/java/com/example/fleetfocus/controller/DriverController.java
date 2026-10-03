package com.example.fleetfocus.controller;

import com.example.fleetfocus.dto.DriverRequestDto;
import com.example.fleetfocus.dto.DriverResponseDto;
import com.example.fleetfocus.dto.DriverStatusUpdateDto;
import com.example.fleetfocus.dto.PageResult;
import com.example.fleetfocus.entity.DriverStatus;
import com.example.fleetfocus.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<DriverResponseDto>> getAllDrivers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) DriverStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageResult<DriverResponseDto> result = driverService.getDrivers(q, status, page, size, sort);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getItems());
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<DriverResponseDto>> getAvailableDrivers() {
        return ResponseEntity.ok(driverService.getAvailableDrivers());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverResponseDto> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(driverService.getDriverByUsername(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<DriverResponseDto> getDriverById(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriverResponseDto> createDriver(
            @Valid @RequestBody DriverRequestDto driver) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(driverService.createDriver(driver));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriverResponseDto> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverRequestDto driver) {
        return ResponseEntity.ok(driverService.updateDriver(id, driver));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<DriverResponseDto> updateDriverStatus(
            @PathVariable Long id,
            @Valid @RequestBody DriverStatusUpdateDto request) {
        return ResponseEntity.ok(driverService.updateDriverStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.ok("Driver deleted successfully");
    }
}
