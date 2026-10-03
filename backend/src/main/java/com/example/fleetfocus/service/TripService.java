package com.example.fleetfocus.service;

import com.example.fleetfocus.dto.PageResult;
import com.example.fleetfocus.dto.TripResponseDto;
import com.example.fleetfocus.entity.*;
import com.example.fleetfocus.exception.ConflictException;
import com.example.fleetfocus.exception.ResourceNotFoundException;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.MaintenanceLogRepository;
import com.example.fleetfocus.repository.TripRepository;
import com.example.fleetfocus.repository.VehicleRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class TripService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "startTime", "endTime", "status", "distanceCovered"
    );

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;

    public TripService(TripRepository tripRepository,
                       VehicleRepository vehicleRepository,
                       DriverRepository driverRepository,
                       MaintenanceLogRepository maintenanceLogRepository) {
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.maintenanceLogRepository = maintenanceLogRepository;
    }

    @Transactional(readOnly = true)
    public List<TripResponseDto> getAllTrips() {
        return tripRepository.findAll().stream()
                .map(TripResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<TripResponseDto> getTrips(String q, TripStatus status, Integer page, Integer size, String sort) {
        Specification<Trip> spec = buildTripSpec(null, q, status);
        return executeTripQuery(spec, page, size, sort);
    }

    @Transactional(readOnly = true)
    public List<TripResponseDto> getTripsForDriver(String username) {
        return tripRepository.findByDriver_Username(username).stream()
                .map(TripResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<TripResponseDto> getTripsForDriver(String username, String q, TripStatus status, Integer page, Integer size, String sort) {
        Specification<Trip> spec = buildTripSpec(username, q, status);
        return executeTripQuery(spec, page, size, sort);
    }

    @Transactional
    public TripResponseDto startTrip(Long vehicleId, Long driverId, Authentication authentication) {
        if (vehicleId == null || driverId == null) {
            throw new IllegalArgumentException("Vehicle ID and Driver ID are required");
        }

        // Lock ordering: Vehicle first, Driver second
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));
        Driver driver = driverRepository.findByIdForUpdate(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));

        if (isDriver(authentication) && !Objects.equals(driver.getUsername(), authentication.getName())) {
            throw new AccessDeniedException("Drivers can only start trips for themselves");
        }

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE
                || tripRepository.existsByVehicle_IdAndStatus(vehicleId, TripStatus.ACTIVE)) {
            throw new ConflictException("Vehicle is not available");
        }
        if (driver.getStatus() != DriverStatus.AVAILABLE
                || tripRepository.existsByDriver_IdAndStatus(driverId, TripStatus.ACTIVE)) {
            throw new ConflictException("Driver is not available");
        }

        vehicle.setStatus(VehicleStatus.ON_TRIP);
        driver.setStatus(DriverStatus.ON_TRIP);
        vehicleRepository.save(vehicle);
        driverRepository.save(driver);

        Trip trip = new Trip();
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setStartTime(LocalDateTime.now());
        trip.setStatus(TripStatus.ACTIVE);
        trip.setDistanceCovered(0.0);

        return TripResponseDto.fromEntity(tripRepository.save(trip));
    }

    @Transactional
    public TripResponseDto endTrip(Long tripId, Double distance, Authentication authentication) {
        if (distance == null || Double.isNaN(distance) || Double.isInfinite(distance)
                || distance < 0.0 || distance > 20000.0) {
            throw new IllegalArgumentException("Distance must be between 0 and 20,000");
        }

        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        if (isDriver(authentication)
                && !Objects.equals(trip.getDriver().getUsername(), authentication.getName())) {
            throw new AccessDeniedException("Drivers can only end their own trips");
        }

        if (trip.getStatus() != TripStatus.ACTIVE) {
            throw new ConflictException("Trip is already completed");
        }

        // Lock Vehicle first, Driver second
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(trip.getVehicle().getId())
                .orElse(trip.getVehicle());
        Driver driver = driverRepository.findByIdForUpdate(trip.getDriver().getId())
                .orElse(trip.getDriver());

        boolean hasOpenMaintenance = maintenanceLogRepository.existsByVehicle_IdAndStatus(
                vehicle.getId(), MaintenanceStatus.IN_PROGRESS);

        if (hasOpenMaintenance) {
            vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
        } else if (vehicle.getStatus() == VehicleStatus.ON_TRIP) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }

        double currentMileage = vehicle.getCurrentMileage() != null ? vehicle.getCurrentMileage() : 0.0;
        vehicle.setCurrentMileage(currentMileage + distance);

        if (driver.getStatus() == DriverStatus.ON_TRIP) {
            driver.setStatus(DriverStatus.AVAILABLE);
        }

        vehicleRepository.save(vehicle);
        driverRepository.save(driver);

        trip.setEndTime(LocalDateTime.now());
        trip.setDistanceCovered(distance);
        trip.setStatus(TripStatus.COMPLETED);

        return TripResponseDto.fromEntity(tripRepository.save(trip));
    }

    private boolean isDriver(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_DRIVER"));
    }

    private Specification<Trip> buildTripSpec(String driverUsername, String q, TripStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Trip, Driver> driverJoin = root.join("driver", JoinType.LEFT);
            Join<Trip, Vehicle> vehicleJoin = root.join("vehicle", JoinType.LEFT);

            if (driverUsername != null) {
                predicates.add(cb.equal(driverJoin.get("username"), driverUsername));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.trim().isEmpty()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(vehicleJoin.get("licensePlate")), pattern),
                        cb.like(cb.lower(vehicleJoin.get("model")), pattern),
                        cb.like(cb.lower(driverJoin.get("name")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PageResult<TripResponseDto> executeTripQuery(Specification<Trip> spec, Integer page, Integer size, String sort) {
        Sort sortObj = parseSort(sort);
        if (page != null) {
            int p = Math.max(0, page);
            int s = size != null ? Math.min(Math.max(1, size), 100) : 20;
            Page<Trip> pageData = tripRepository.findAll(spec, PageRequest.of(p, s, sortObj));
            List<TripResponseDto> dtos = pageData.getContent().stream()
                    .map(TripResponseDto::fromEntity)
                    .toList();
            return new PageResult<>(dtos, pageData.getTotalElements());
        } else {
            List<TripResponseDto> dtos = tripRepository.findAll(spec, sortObj).stream()
                    .map(TripResponseDto::fromEntity)
                    .toList();
            return new PageResult<>(dtos, dtos.size());
        }
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            field = "id";
        }
        Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
}
