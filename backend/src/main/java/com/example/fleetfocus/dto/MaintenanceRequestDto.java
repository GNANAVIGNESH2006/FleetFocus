package com.example.fleetfocus.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequestDto {

    @NotNull(message = "Service date is required")
    @PastOrPresent(message = "Service date cannot be in the future")
    private LocalDate serviceDate;

    @NotBlank(message = "Maintenance description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Maintenance cost is required")
    @DecimalMin(value = "0.00", message = "Cost cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Cost must have at most 10 integer digits and 2 decimal places")
    private BigDecimal cost;

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }
}
