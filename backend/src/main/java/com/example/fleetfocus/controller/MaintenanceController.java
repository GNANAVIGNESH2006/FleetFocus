package com.example.fleetfocus.controller;

import com.example.fleetfocus.entity.MaintenanceLog;
import com.example.fleetfocus.service.MaintenanceService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500",
        "http://localhost:63342"
})
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<MaintenanceLog>> getAllLogs() {
        return ResponseEntity.ok(maintenanceService.getAllLogs());
    }

    @PostMapping("/log/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<MaintenanceLog> logMaintenance(
            @PathVariable Long vehicleId,
            @Valid @RequestBody MaintenanceLog log,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.logMaintenance(vehicleId, log, authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMaintenance(@PathVariable Long id) {

        maintenanceService.deleteMaintenance(id);

        return ResponseEntity.noContent().build();
    }
}

