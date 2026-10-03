package com.example.fleetfocus.controller;

import com.example.fleetfocus.dto.PageResult;
import com.example.fleetfocus.dto.StatusUpdateDto;
import com.example.fleetfocus.dto.VehicleRequestDto;
import com.example.fleetfocus.dto.VehicleResponseDto;
import com.example.fleetfocus.entity.VehicleStatus;
import com.example.fleetfocus.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<VehicleResponseDto>> getAllVehicles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageResult<VehicleResponseDto> result = vehicleService.getVehicles(q, status, page, size, sort);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getItems());
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<List<VehicleResponseDto>> getAvailableVehicles(Authentication authentication) {
        return ResponseEntity.ok(vehicleService.getAvailableVehicles(authentication));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<VehicleResponseDto> getMyVehicle(Authentication authentication) {
        return ResponseEntity.ok(vehicleService.getVehicleForDriver(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<VehicleResponseDto> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleResponseDto> createVehicle(
            @Valid @RequestBody VehicleRequestDto vehicle) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.createVehicle(vehicle));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleResponseDto> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequestDto vehicle) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, vehicle));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<VehicleResponseDto> updateVehicleStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto request) {
        return ResponseEntity.ok(vehicleService.updateVehicleStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok("Vehicle deleted successfully");
    }
}
