package com.example.fleetfocus.controller;

import com.example.fleetfocus.dto.CreateUserDto;
import com.example.fleetfocus.dto.PageResult;
import com.example.fleetfocus.dto.RoleUpdateDto;
import com.example.fleetfocus.dto.UserResponseDto;
import com.example.fleetfocus.entity.UserRole;
import com.example.fleetfocus.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserRole status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        UserRole effectiveRole = role != null ? role : status;
        PageResult<UserResponseDto> result = userService.getUsers(q, effectiveRole, page, size, sort);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getItems());
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody CreateUserDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(request));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponseDto> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateDto request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateUserRole(id, request.getRole(), authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, Authentication authentication) {
        userService.deleteUser(id, authentication);
        return ResponseEntity.ok("User deleted successfully");
    }
}
