package com.example.fleetfocus.dto;

import com.example.fleetfocus.entity.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DriverRequestDto {

    @NotBlank(message = "Driver name is required")
    @Size(min = 3, max = 50, message = "Driver name must be between 3 and 50 characters")
    private String name;

    @NotBlank(message = "License number is required")
    @Pattern(
            regexp = "^[A-Za-z0-9-]{5,30}$",
            message = "License number must be 5-30 characters and contain only letters, digits, or hyphens"
    )
    private String licenseNumber;

    private DriverStatus status;

    @Pattern(
            regexp = "^$|^[A-Za-z0-9._-]{3,30}$",
            message = "Username must be 3-30 characters and contain only letters, digits, dots, underscores, or hyphens"
    )
    private String username;

    public DriverRequestDto(String name, String licenseNumber, DriverStatus status, String username) {
        setName(name);
        setLicenseNumber(licenseNumber);
        this.status = status;
        setUsername(username);
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim().toUpperCase() : null;
    }

    public void setUsername(String username) {
        if (username == null) {
            this.username = null;
        } else {
            String trimmed = username.trim();
            this.username = trimmed.isEmpty() ? null : trimmed;
        }
    }
}
