package com.example.fleetfocus.service;

import com.example.fleetfocus.dto.CreateUserDto;
import com.example.fleetfocus.dto.PageResult;
import com.example.fleetfocus.dto.UserResponseDto;
import com.example.fleetfocus.entity.SystemUser;
import com.example.fleetfocus.entity.UserRole;
import com.example.fleetfocus.exception.ConflictException;
import com.example.fleetfocus.exception.ResourceNotFoundException;
import com.example.fleetfocus.repository.DriverRepository;
import com.example.fleetfocus.repository.SystemUserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "username", "email", "role"
    );

    private final SystemUserRepository systemUserRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(SystemUserRepository systemUserRepository,
                       DriverRepository driverRepository,
                       PasswordEncoder passwordEncoder) {
        this.systemUserRepository = systemUserRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return systemUserRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<UserResponseDto> getUsers(String q, UserRole role, Integer page, Integer size, String sort) {
        Specification<SystemUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (q != null && !q.trim().isEmpty()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sortObj = parseSort(sort);
        if (page != null) {
            int p = Math.max(0, page);
            int s = size != null ? Math.min(Math.max(1, size), 100) : 20;
            Page<SystemUser> pageData = systemUserRepository.findAll(spec, PageRequest.of(p, s, sortObj));
            List<UserResponseDto> dtos = pageData.getContent().stream()
                    .map(this::toDto)
                    .toList();
            return new PageResult<>(dtos, pageData.getTotalElements());
        } else {
            List<UserResponseDto> dtos = systemUserRepository.findAll(spec, sortObj).stream()
                    .map(this::toDto)
                    .toList();
            return new PageResult<>(dtos, dtos.size());
        }
    }

    @Transactional
    public UserResponseDto createUser(CreateUserDto request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (systemUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already taken");
        }
        if (systemUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered");
        }

        SystemUser user = new SystemUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(email);
        user.setRole(request.getRole());

        return toDto(systemUserRepository.save(user));
    }

    @Transactional
    public UserResponseDto updateUserRole(Long id, UserRole newRole, Authentication authentication) {
        if (newRole == null) {
            throw new IllegalArgumentException("Role is required");
        }

        SystemUser user = systemUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (authentication != null
                && user.getUsername().equalsIgnoreCase(authentication.getName())
                && newRole != UserRole.ADMIN) {
            throw new ConflictException("You cannot demote your own admin account");
        }

        if (user.getRole() == UserRole.ADMIN
                && newRole != UserRole.ADMIN
                && systemUserRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new ConflictException("Cannot demote the last ADMIN user");
        }

        if (user.getRole() == UserRole.DRIVER && newRole != UserRole.DRIVER) {
            driverRepository.findByUsernameIgnoreCase(user.getUsername())
                    .ifPresent(driver -> {
                        driver.setUsername(null);
                        driverRepository.save(driver);
                    });
        }

        user.setRole(newRole);
        return toDto(systemUserRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id, Authentication authentication) {
        SystemUser user = systemUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (authentication != null
                && user.getUsername().equalsIgnoreCase(authentication.getName())) {
            throw new ConflictException("You cannot delete your own account");
        }

        if (user.getRole() == UserRole.ADMIN
                && systemUserRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new ConflictException("Cannot delete the last ADMIN user");
        }

        driverRepository.findByUsernameIgnoreCase(user.getUsername())
                .ifPresent(driver -> {
                    driver.setUsername(null);
                    driverRepository.save(driver);
                });

        systemUserRepository.delete(user);
    }

    private UserResponseDto toDto(SystemUser user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
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
