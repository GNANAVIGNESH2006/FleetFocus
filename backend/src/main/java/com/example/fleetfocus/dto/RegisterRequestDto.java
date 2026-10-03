package com.example.fleetfocus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterRequestDto {

    @NotBlank(message = "Username is required")
    @Pattern(
            regexp = "^[A-Za-z0-9._-]{3,30}$",
            message = "Username must be 3-30 characters and contain only letters, digits, dots, underscores, or hyphens"
    )
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    public RegisterRequestDto(String username, String password, String email) {
        setUsername(username);
        setPassword(password);
        setEmail(email);
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim() : null;
    }
}