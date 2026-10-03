package com.example.fleetfocus.controller;

import com.example.fleetfocus.dto.EndTripRequestDto;
import com.example.fleetfocus.dto.PageResult;
import com.example.fleetfocus.dto.StartTripRequestDto;
import com.example.fleetfocus.dto.TripResponseDto;
import com.example.fleetfocus.entity.TripStatus;
import com.example.fleetfocus.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<TripResponseDto>> getAllTrips(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageResult<TripResponseDto> result = tripService.getTrips(q, status, page, size, sort);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getItems());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<TripResponseDto>> getMyTrips(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            Authentication authentication) {
        PageResult<TripResponseDto> result = tripService.getTripsForDriver(
                authentication.getName(), q, status, page, size, sort);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getItems());
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<TripResponseDto> startTrip(
            @Valid @RequestBody StartTripRequestDto request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripService.startTrip(request.getVehicleId(), request.getDriverId(), authentication));
    }

    @PutMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<TripResponseDto> endTrip(
            @PathVariable Long id,
            @Valid @RequestBody EndTripRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(tripService.endTrip(id, request.getDistance(), authentication));
    }
}
