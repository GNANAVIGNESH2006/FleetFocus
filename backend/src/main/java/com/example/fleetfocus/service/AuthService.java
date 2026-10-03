package com.example.fleetfocus.service;

import com.example.fleetfocus.dto.AuthRequestDto;
import com.example.fleetfocus.dto.AuthResponseDto;
import com.example.fleetfocus.dto.RegisterRequestDto;
import com.example.fleetfocus.entity.SystemUser;
import com.example.fleetfocus.entity.UserRole;
import com.example.fleetfocus.exception.ConflictException;
import com.example.fleetfocus.repository.SystemUserRepository;
import com.example.fleetfocus.security.JwtService;
import com.example.fleetfocus.security.LoginAttemptService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final SystemUserRepository systemUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    public AuthService(SystemUserRepository systemUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       LoginAttemptService loginAttemptService) {
        this.systemUserRepository = systemUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional(readOnly = true)
    public AuthResponseDto authenticate(AuthRequestDto request) {
        return authenticate(request, "unknown");
    }

    @Transactional(readOnly = true)
    public AuthResponseDto authenticate(AuthRequestDto request, String clientIp) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";

        loginAttemptService.checkAllowed(username, clientIp);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(username, clientIp);
            throw ex;
        }

        SystemUser user = systemUserRepository.findByUsername(username)
                .or(() -> systemUserRepository.findByUsernameIgnoreCase(username))
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(username, clientIp);
                    return new BadCredentialsException("Invalid username or password");
                });

        loginAttemptService.recordSuccess(username, clientIp);

        String token = jwtService.generateToken(user);

        return new AuthResponseDto(token, user.getUsername(), user.getRole().name());
    }

    @Transactional
    public void register(RegisterRequestDto request) {
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
        user.setRole(UserRole.DRIVER);
        systemUserRepository.save(user);
    }
}
