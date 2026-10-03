package com.example.fleetfocus.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Driver name is required")
    @Size(min = 3, max = 50, message = "Driver name must be between 3 and 50 characters")
    @Column(nullable = false, length = 50)
    private String name;

    @NotBlank(message = "License number is required")
    @Pattern(
            regexp = "^[A-Za-z0-9-]{5,30}$",
            message = "License number must be 5-30 characters and contain only letters, digits, or hyphens"
    )
    @Column(name = "license_number", unique = true, nullable = false, length = 30)
    private String licenseNumber;

    @NotNull(message = "Driver status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus status;

    @Column(name = "username", unique = true, length = 30)
    private String username;
}
