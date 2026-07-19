package com.example.fleetfocus.controller;

import com.example.fleetfocus.entity.Trip;
import com.example.fleetfocus.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500",
        "http://localhost:63342"
})
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<Trip>> getAllTrips() {
        return ResponseEntity.ok(tripService.getAllTrips());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<Trip>> getMyTrips(Authentication authentication) {
        return ResponseEntity.ok(tripService.getTripsForDriver(authentication.getName()));
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<Trip> startTrip(@RequestBody Map<String, Long> request, Authentication authentication) {
        Long vehicleId = request.get("vehicleId");
        Long driverId = request.get("driverId");
        return ResponseEntity.ok(tripService.startTrip(vehicleId, driverId, authentication));
    }

    @PutMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<Trip> endTrip(@PathVariable Long id, @RequestBody Map<String, Double> request,
                                         Authentication authentication) {
        Double distance = request.get("distance");
        return ResponseEntity.ok(tripService.endTrip(id, distance, authentication));
    }
}

