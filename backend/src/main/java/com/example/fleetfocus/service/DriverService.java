package com.example.fleetfocus.service;

import com.example.fleetfocus.dto.DriverRequestDto;
import com.example.fleetfocus.dto.DriverResponseDto;
import com.example.fleetfocus.dto.PageResult;
import com.example.fleetfocus.entity.*;
import com.example.fleetfocus.exception.ConflictException;
import com.example.fleetfocus.exception.ResourceNotFoundException;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.SystemUserRepository;
import com.example.fleetfocus.repository.TripRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class DriverService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "licenseNumber", "status", "username"
    );

    private final DriverRepository driverRepository;
    private final SystemUserRepository systemUserRepository;
    private final TripRepository tripRepository;

    public DriverService(DriverRepository driverRepository,
                         SystemUserRepository systemUserRepository,
                         TripRepository tripRepository) {
        this.driverRepository = driverRepository;
        this.systemUserRepository = systemUserRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public List<DriverResponseDto> getAllDrivers() {
        return driverRepository.findAll().stream()
                .map(DriverResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<DriverResponseDto> getDrivers(String q, DriverStatus status, Integer page, Integer size, String sort) {
        Specification<Driver> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.trim().isEmpty()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("licenseNumber")), pattern),
                        cb.like(cb.lower(root.get("username")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sortObj = parseSort(sort);
        if (page != null) {
            int p = Math.max(0, page);
            int s = size != null ? Math.min(Math.max(1, size), 100) : 20;
            Page<Driver> pageData = driverRepository.findAll(spec, PageRequest.of(p, s, sortObj));
            List<DriverResponseDto> dtos = pageData.getContent().stream()
                    .map(DriverResponseDto::fromEntity)
                    .toList();
            return new PageResult<>(dtos, pageData.getTotalElements());
        } else {
            List<DriverResponseDto> dtos = driverRepository.findAll(spec, sortObj).stream()
                    .map(DriverResponseDto::fromEntity)
                    .toList();
            return new PageResult<>(dtos, dtos.size());
        }
    }

    @Transactional(readOnly = true)
    public List<DriverResponseDto> getAvailableDrivers() {
        return driverRepository.findByStatus(DriverStatus.AVAILABLE).stream()
                .map(DriverResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DriverResponseDto getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        return DriverResponseDto.fromEntity(driver);
    }

    @Transactional(readOnly = true)
    public DriverResponseDto getDriverByUsername(String username) {
        Driver driver = driverRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No driver profile is linked to this account"));
        return DriverResponseDto.fromEntity(driver);
    }

    @Transactional
    public DriverResponseDto createDriver(DriverRequestDto request) {
        String name = request.getName().trim();
        String licenseNumber = request.getLicenseNumber().trim().toUpperCase();
        DriverStatus status = request.getStatus() != null ? request.getStatus() : DriverStatus.AVAILABLE;

        if (status == DriverStatus.ON_TRIP) {
            throw new ConflictException("Cannot manually set driver status to ON_TRIP");
        }
        if (driverRepository.existsByLicenseNumberIgnoreCase(licenseNumber)) {
            throw new ConflictException("Driver with this license number already exists");
        }

        String canonicalUsername = resolveAndValidateUsername(request.getUsername(), null);

        Driver driver = new Driver();
        driver.setName(name);
        driver.setLicenseNumber(licenseNumber);
        driver.setStatus(status);
        driver.setUsername(canonicalUsername);

        return DriverResponseDto.fromEntity(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponseDto updateDriver(Long id, DriverRequestDto request) {
        Driver driver = driverRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        String name = request.getName().trim();
        String licenseNumber = request.getLicenseNumber().trim().toUpperCase();

        if (driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot(licenseNumber, id)) {
            throw new ConflictException("Driver with this license number already exists");
        }

        boolean isOnTrip = driver.getStatus() == DriverStatus.ON_TRIP
                || tripRepository.existsByDriver_IdAndStatus(id, TripStatus.ACTIVE);

        if (request.getStatus() != null) {
            if (isOnTrip) {
                if (request.getStatus() != DriverStatus.ON_TRIP) {
                    throw new ConflictException("Cannot change status of a driver who is currently on an active trip");
                }
            } else {
                if (request.getStatus() == DriverStatus.ON_TRIP) {
                    throw new ConflictException("Cannot manually set driver status to ON_TRIP");
                }
                driver.setStatus(request.getStatus());
            }
        }

        String canonicalUsername = resolveAndValidateUsername(request.getUsername(), id);

        driver.setName(name);
        driver.setLicenseNumber(licenseNumber);
        driver.setUsername(canonicalUsername);

        return DriverResponseDto.fromEntity(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponseDto updateDriverStatus(Long id, DriverStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Driver status is required");
        }
        if (newStatus == DriverStatus.ON_TRIP) {
            throw new ConflictException("Cannot manually set driver status to ON_TRIP");
        }

        Driver driver = driverRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        if (driver.getStatus() == DriverStatus.ON_TRIP
                || tripRepository.existsByDriver_IdAndStatus(id, TripStatus.ACTIVE)) {
            throw new ConflictException("Cannot change status of a driver who is currently on an active trip");
        }

        driver.setStatus(newStatus);
        return DriverResponseDto.fromEntity(driverRepository.save(driver));
    }

    @Transactional
    public void deleteDriver(Long id) {
        Driver driver = driverRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        if (driver.getStatus() == DriverStatus.ON_TRIP
                || tripRepository.existsByDriver_IdAndStatus(id, TripStatus.ACTIVE)) {
            throw new ConflictException("Cannot delete a driver who is currently on an active trip");
        }

        if (tripRepository.existsByDriver_Id(id)) {
            throw new ConflictException("Driver has trip history and cannot be deleted");
        }

        driverRepository.delete(driver);
    }

    private String resolveAndValidateUsername(String rawUsername, Long currentDriverId) {
        if (rawUsername == null || rawUsername.trim().isEmpty()) {
            return null;
        }
        String trimmed = rawUsername.trim();
        SystemUser user = systemUserRepository.findByUsernameIgnoreCase(trimmed)
                .orElseThrow(() -> new IllegalArgumentException("No DRIVER account with that username"));

        if (user.getRole() != UserRole.DRIVER) {
            throw new IllegalArgumentException("No DRIVER account with that username");
        }

        Optional<Driver> existingLink = driverRepository.findByUsernameIgnoreCase(user.getUsername());
        if (existingLink.isPresent()
                && (currentDriverId == null || !existingLink.get().getId().equals(currentDriverId))) {
            throw new ConflictException("Username is already linked to another driver");
        }

        return user.getUsername();
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
