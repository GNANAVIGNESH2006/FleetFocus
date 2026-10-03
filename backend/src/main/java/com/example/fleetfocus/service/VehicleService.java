package com.example.fleetfocus.service;

import com.example.fleetfocus.dto.PageResult;
import com.example.fleetfocus.dto.VehicleRequestDto;
import com.example.fleetfocus.dto.VehicleResponseDto;
import com.example.fleetfocus.entity.*;
import com.example.fleetfocus.exception.ConflictException;
import com.example.fleetfocus.exception.ResourceNotFoundException;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.MaintenanceLogRepository;
import com.example.fleetfocus.repository.TripRepository;
import com.example.fleetfocus.repository.VehicleRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class VehicleService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "vin", "licensePlate", "model", "status", "currentMileage"
    );

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          DriverRepository driverRepository,
                          TripRepository tripRepository,
                          MaintenanceLogRepository maintenanceLogRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
        this.maintenanceLogRepository = maintenanceLogRepository;
    }

    @Transactional(readOnly = true)
    public List<VehicleResponseDto> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(VehicleResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<VehicleResponseDto> getVehicles(String q, VehicleStatus status, Integer page, Integer size, String sort) {
        Specification<Vehicle> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.trim().isEmpty()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("vin")), pattern),
                        cb.like(cb.lower(root.get("licensePlate")), pattern),
                        cb.like(cb.lower(root.get("model")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sortObj = parseSort(sort);
        if (page != null) {
            int p = Math.max(0, page);
            int s = size != null ? Math.min(Math.max(1, size), 100) : 20;
            Page<Vehicle> pageData = vehicleRepository.findAll(spec, PageRequest.of(p, s, sortObj));
            List<VehicleResponseDto> dtos = pageData.getContent().stream()
                    .map(VehicleResponseDto::fromEntity)
                    .toList();
            return new PageResult<>(dtos, pageData.getTotalElements());
        } else {
            List<VehicleResponseDto> dtos = vehicleRepository.findAll(spec, sortObj).stream()
                    .map(VehicleResponseDto::fromEntity)
                    .toList();
            return new PageResult<>(dtos, dtos.size());
        }
    }

    @Transactional(readOnly = true)
    public List<VehicleResponseDto> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream()
                .map(VehicleResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponseDto> getAvailableVehicles(Authentication authentication) {
        boolean isDriverOnly = isDriverOnly(authentication);
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream()
                .map(v -> isDriverOnly ? VehicleResponseDto.summary(v) : VehicleResponseDto.fromEntity(v))
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponseDto getVehicleById(Long id) {
        return VehicleResponseDto.fromEntity(getVehicleEntityById(id));
    }

    @Transactional(readOnly = true)
    public VehicleResponseDto getVehicleForDriver(String username) {
        Driver driver = driverRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No driver profile is linked to this account"));

        List<Trip> activeTrips = tripRepository.findByDriver_UsernameAndStatus(username, TripStatus.ACTIVE);

        if (activeTrips.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Driver '" + driver.getName() + "' has no vehicle assigned right now");
        }

        return VehicleResponseDto.fromEntity(activeTrips.get(0).getVehicle());
    }

    @Transactional
    public VehicleResponseDto createVehicle(VehicleRequestDto request) {
        String vin = request.getVin().trim().toUpperCase();
        String licensePlate = request.getLicensePlate().trim().toUpperCase();
        String model = request.getModel().trim();
        VehicleStatus status = request.getStatus() != null ? request.getStatus() : VehicleStatus.AVAILABLE;
        Double mileage = request.getCurrentMileage() != null ? request.getCurrentMileage() : 0.0;

        if (status == VehicleStatus.ON_TRIP) {
            throw new ConflictException("Cannot manually set vehicle status to ON_TRIP");
        }
        if (vehicleRepository.existsByVinIgnoreCase(vin)) {
            throw new ConflictException("Vehicle with this VIN already exists");
        }
        if (vehicleRepository.existsByLicensePlateIgnoreCase(licensePlate)) {
            throw new ConflictException("Vehicle with this license plate already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vin);
        vehicle.setLicensePlate(licensePlate);
        vehicle.setModel(model);
        vehicle.setStatus(status);
        vehicle.setCurrentMileage(mileage);

        return VehicleResponseDto.fromEntity(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponseDto updateVehicle(Long id, VehicleRequestDto request) {
        Vehicle existing = vehicleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        String vin = request.getVin().trim().toUpperCase();
        String licensePlate = request.getLicensePlate().trim().toUpperCase();
        String model = request.getModel().trim();
        Double mileage = request.getCurrentMileage() != null ? request.getCurrentMileage() : existing.getCurrentMileage();

        if (vehicleRepository.existsByVinIgnoreCaseAndIdNot(vin, id)) {
            throw new ConflictException("Vehicle with this VIN already exists");
        }
        if (vehicleRepository.existsByLicensePlateIgnoreCaseAndIdNot(licensePlate, id)) {
            throw new ConflictException("Vehicle with this license plate already exists");
        }

        boolean isOnTrip = existing.getStatus() == VehicleStatus.ON_TRIP
                || tripRepository.existsByVehicle_IdAndStatus(id, TripStatus.ACTIVE);

        if (request.getStatus() != null) {
            if (isOnTrip) {
                if (request.getStatus() != VehicleStatus.ON_TRIP) {
                    throw new ConflictException("Cannot change status of a vehicle that is currently on an active trip");
                }
            } else {
                if (request.getStatus() == VehicleStatus.ON_TRIP) {
                    throw new ConflictException("Cannot manually set vehicle status to ON_TRIP");
                }
                if (request.getStatus() == VehicleStatus.AVAILABLE
                        && maintenanceLogRepository.existsByVehicle_IdAndStatus(id, MaintenanceStatus.IN_PROGRESS)) {
                    throw new ConflictException("Cannot set vehicle status to AVAILABLE while maintenance is in progress");
                }
                existing.setStatus(request.getStatus());
            }
        }

        existing.setVin(vin);
        existing.setLicensePlate(licensePlate);
        existing.setModel(model);
        existing.setCurrentMileage(mileage);

        return VehicleResponseDto.fromEntity(vehicleRepository.save(existing));
    }

    @Transactional
    public VehicleResponseDto updateVehicleStatus(Long id, VehicleStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Vehicle status is required");
        }
        if (newStatus == VehicleStatus.ON_TRIP) {
            throw new ConflictException("Cannot manually set vehicle status to ON_TRIP");
        }

        Vehicle existing = vehicleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (existing.getStatus() == VehicleStatus.ON_TRIP
                || tripRepository.existsByVehicle_IdAndStatus(id, TripStatus.ACTIVE)) {
            throw new ConflictException("Cannot change status of a vehicle that is currently on an active trip");
        }

        if (newStatus == VehicleStatus.AVAILABLE
                && maintenanceLogRepository.existsByVehicle_IdAndStatus(id, MaintenanceStatus.IN_PROGRESS)) {
            throw new ConflictException("Cannot set vehicle status to AVAILABLE while maintenance is in progress");
        }

        existing.setStatus(newStatus);
        return VehicleResponseDto.fromEntity(vehicleRepository.save(existing));
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (vehicle.getStatus() == VehicleStatus.ON_TRIP
                || tripRepository.existsByVehicle_IdAndStatus(id, TripStatus.ACTIVE)) {
            throw new ConflictException("Cannot delete a vehicle that is currently on an active trip");
        }

        if (tripRepository.existsByVehicle_Id(id) || maintenanceLogRepository.existsByVehicle_Id(id)) {
            throw new ConflictException("Vehicle has trip or maintenance history and cannot be deleted");
        }

        vehicleRepository.delete(vehicle);
    }

    private Vehicle getVehicleEntityById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
    }

    private boolean isDriverOnly(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        boolean hasDriver = false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role) || "ROLE_DISPATCHER".equals(role)) {
                return false;
            }
            if ("ROLE_DRIVER".equals(role)) {
                hasDriver = true;
            }
        }
        return hasDriver;
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "id");
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
