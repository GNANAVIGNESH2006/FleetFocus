package com.example.fleetfocus.controller;

import com.example.fleetfocus.dto.AuthRequestDto;
import com.example.fleetfocus.dto.AuthResponseDto;
import com.example.fleetfocus.dto.RegisterRequestDto;
import com.example.fleetfocus.entity.SystemUser;
import com.example.fleetfocus.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody AuthRequestDto request,
            HttpServletRequest servletRequest) {
        String clientIp = servletRequest != null ? servletRequest.getRemoteAddr() : "unknown";
        return ResponseEntity.ok(authService.authenticate(request, clientIp));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequestDto request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(Authentication authentication) {
        String username = authentication.getName();
        String role;
        if (authentication.getPrincipal() instanceof SystemUser systemUser && systemUser.getRole() != null) {
            username = systemUser.getUsername();
            role = systemUser.getRole().name();
        } else {
            role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .map(a -> a.replace("ROLE_", ""))
                    .orElse("");
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("role", role);
        return ResponseEntity.ok(body);
    }
}