package com.example.fleetfocus.controller;

import com.example.fleetfocus.dto.MaintenanceRequestDto;
import com.example.fleetfocus.dto.MaintenanceResponseDto;
import com.example.fleetfocus.dto.PageResult;
import com.example.fleetfocus.entity.MaintenanceStatus;
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
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<MaintenanceResponseDto>> getAllLogs(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageResult<MaintenanceResponseDto> result = maintenanceService.getLogs(vehicleId, status, q, page, size, sort);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getItems());
    }

    @PostMapping("/log/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<MaintenanceResponseDto> logMaintenance(
            @PathVariable Long vehicleId,
            @Valid @RequestBody MaintenanceRequestDto log,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.logMaintenance(vehicleId, log, authentication));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<MaintenanceResponseDto> completeMaintenance(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.completeMaintenance(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMaintenance(@PathVariable Long id) {
        maintenanceService.deleteMaintenance(id);
        return ResponseEntity.noContent().build();
    }
}
