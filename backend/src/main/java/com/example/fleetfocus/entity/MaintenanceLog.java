package com.example.fleetfocus.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @NotNull(message = "Service date is required")
    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @NotBlank(message = "Maintenance description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String description;

    @NotNull(message = "Maintenance cost is required")
    @DecimalMin(value = "0.00", message = "Cost cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Cost must have at most 10 integer digits and 2 decimal places")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @NotNull(message = "Maintenance status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(32) default 'IN_PROGRESS'")
    private MaintenanceStatus status = MaintenanceStatus.IN_PROGRESS;

    @Column(name = "completed_date")
    private LocalDate completedDate;
}