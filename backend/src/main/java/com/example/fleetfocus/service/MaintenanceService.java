package com.example.fleetfocus.service;

import com.example.fleetfocus.dto.MaintenanceRequestDto;
import com.example.fleetfocus.dto.MaintenanceResponseDto;
import com.example.fleetfocus.dto.PageResult;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class MaintenanceService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "serviceDate", "cost", "status", "completedDate"
    );

    private final MaintenanceLogRepository maintenanceLogRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;

    public MaintenanceService(MaintenanceLogRepository maintenanceLogRepository,
                              VehicleRepository vehicleRepository,
                              DriverRepository driverRepository,
                              TripRepository tripRepository) {
        this.maintenanceLogRepository = maintenanceLogRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponseDto> getAllLogs() {
        return maintenanceLogRepository.findAll().stream()
                .map(MaintenanceResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<MaintenanceResponseDto> getLogs(Long vehicleId, MaintenanceStatus status, String q,
                                                      Integer page, Integer size, String sort) {
        Specification<MaintenanceLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<MaintenanceLog, Vehicle> vehicleJoin = root.join("vehicle", JoinType.LEFT);

            if (vehicleId != null) {
                predicates.add(cb.equal(vehicleJoin.get("id"), vehicleId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.trim().isEmpty()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(vehicleJoin.get("licensePlate")), pattern),
                        cb.like(cb.lower(vehicleJoin.get("model")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sortObj = parseSort(sort);
        if (page != null) {
            int p = Math.max(0, page);
            int s = size != null ? Math.min(Math.max(1, size), 100) : 20;
            Page<MaintenanceLog> pageData = maintenanceLogRepository.findAll(spec, PageRequest.of(p, s, sortObj));
            List<MaintenanceResponseDto> dtos = pageData.getContent().stream()
                    .map(MaintenanceResponseDto::fromEntity)
                    .toList();
            return new PageResult<>(dtos, pageData.getTotalElements());
        } else {
            List<MaintenanceResponseDto> dtos = maintenanceLogRepository.findAll(spec, sortObj).stream()
                    .map(MaintenanceResponseDto::fromEntity)
                    .toList();
            return new PageResult<>(dtos, dtos.size());
        }
    }

    @Transactional
    public MaintenanceResponseDto logMaintenance(Long vehicleId, MaintenanceRequestDto request, Authentication authentication) {
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        if (isDriver(authentication)) {
            Driver driver = driverRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No driver profile is linked to this account"));

            List<Trip> activeTrips = tripRepository.findByDriver_UsernameAndStatus(
                    authentication.getName(), TripStatus.ACTIVE);

            boolean ownsVehicle = activeTrips.stream()
                    .anyMatch(t -> t.getVehicle().getId().equals(vehicleId));

            if (!ownsVehicle) {
                throw new AccessDeniedException(
                        "Driver '" + driver.getName() + "' can only report issues for their own assigned vehicle");
            }
        }

        if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
            vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
            vehicleRepository.save(vehicle);
        }

        MaintenanceLog log = new MaintenanceLog();
        log.setVehicle(vehicle);
        log.setDescription(request.getDescription().trim());
        log.setServiceDate(request.getServiceDate());
        log.setCost(request.getCost() != null ? request.getCost() : java.math.BigDecimal.ZERO);
        log.setStatus(MaintenanceStatus.IN_PROGRESS);
        log.setCompletedDate(null);

        return MaintenanceResponseDto.fromEntity(maintenanceLogRepository.save(log));
    }

    @Transactional
    public MaintenanceResponseDto completeMaintenance(Long id) {
        MaintenanceLog log = maintenanceLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance log not found with id: " + id));

        if (log.getStatus() == MaintenanceStatus.COMPLETED) {
            throw new ConflictException("Maintenance log is already completed");
        }

        log.setStatus(MaintenanceStatus.COMPLETED);
        log.setCompletedDate(LocalDate.now());
        MaintenanceLog savedLog = maintenanceLogRepository.saveAndFlush(log);

        Long vehicleId = savedLog.getVehicle().getId();
        vehicleRepository.findByIdForUpdate(vehicleId).ifPresent(vehicle -> {
            boolean hasOtherOpen = maintenanceLogRepository.existsByVehicle_IdAndStatus(
                    vehicleId, MaintenanceStatus.IN_PROGRESS);
            boolean hasActiveTrip = tripRepository.existsByVehicle_IdAndStatus(
                    vehicleId, TripStatus.ACTIVE);
            if (!hasOtherOpen && !hasActiveTrip && vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicle);
            }
        });

        return MaintenanceResponseDto.fromEntity(savedLog);
    }

    @Transactional
    public void deleteMaintenance(Long id) {
        MaintenanceLog log = maintenanceLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance log not found with id: " + id));

        boolean wasInProgress = log.getStatus() == MaintenanceStatus.IN_PROGRESS;
        Long vehicleId = log.getVehicle().getId();

        maintenanceLogRepository.delete(log);
        maintenanceLogRepository.flush();

        if (wasInProgress) {
            vehicleRepository.findByIdForUpdate(vehicleId).ifPresent(vehicle -> {
                boolean hasOtherOpen = maintenanceLogRepository.existsByVehicle_IdAndStatus(
                        vehicleId, MaintenanceStatus.IN_PROGRESS);
                boolean hasActiveTrip = tripRepository.existsByVehicle_IdAndStatus(
                        vehicleId, TripStatus.ACTIVE);
                if (!hasOtherOpen && !hasActiveTrip && vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
                    vehicle.setStatus(VehicleStatus.AVAILABLE);
                    vehicleRepository.save(vehicle);
                }
            });
        }
    }

    private boolean isDriver(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_DRIVER"));
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
