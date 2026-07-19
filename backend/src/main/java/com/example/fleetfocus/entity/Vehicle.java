package com.example.fleetfocus.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "VIN is required")
    @Size(min = 10, max = 10, message = "VIN must contain exactly 10 characters")
    @Column(unique = true, nullable = false, length = 17)
    private String vin;

    @NotBlank(message = "License plate is required")
    @Size(min = 4, max = 15, message = "License plate must be between 4 and 15 characters")
    @Column(name = "license_plate", unique = true, nullable = false)
    private String licensePlate;

    @NotBlank(message = "Vehicle model is required")
    @Size(min = 2, max = 50, message = "Model must be between 2 and 50 characters")
    @Column(nullable = false)
    private String model;

    @NotNull(message = "Vehicle status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @NotNull(message = "Current mileage is required")
    @PositiveOrZero(message = "Mileage cannot be negative")
    @Column(name = "current_mileage")
    private Double currentMileage;
}