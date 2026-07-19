package com.example.fleetfocus.service;

import com.example.fleetfocus.dto.UserResponseDto;
import com.example.fleetfocus.entity.SystemUser;
import com.example.fleetfocus.entity.UserRole;
import com.example.fleetfocus.exception.ResourceNotFoundException;
import com.example.fleetfocus.repository.SystemUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final SystemUserRepository systemUserRepository;

    public UserService(SystemUserRepository systemUserRepository) {
        this.systemUserRepository = systemUserRepository;
    }

    public List<UserResponseDto> getAllUsers() {
        return systemUserRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public UserResponseDto updateUserRole(Long id, String role) {
        SystemUser user = systemUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setRole(UserRole.valueOf(role.toUpperCase()));
        return toDto(systemUserRepository.save(user));
    }

    public void deleteUser(Long id) {
        SystemUser user = systemUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

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
}

