package com.example.fleetfocus.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EndTripRequestDto {

    @NotNull(message = "Distance covered is required")
    @PositiveOrZero(message = "Distance cannot be negative")
    @DecimalMax(value = "100000.0", message = "Distance cannot exceed 100,000")
    private Double distance;
}
