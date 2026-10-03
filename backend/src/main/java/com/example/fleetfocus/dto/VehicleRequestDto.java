package com.example.fleetfocus.dto;

import com.example.fleetfocus.entity.VehicleStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VehicleRequestDto {

    @NotBlank(message = "VIN is required")
    @Pattern(
            regexp = "^(?i)[A-HJ-NPR-Z0-9]{17}$",
            message = "VIN must contain 17 valid characters (letters A-Z except I, O, Q, and digits 0-9)"
    )
    private String vin;

    @NotBlank(message = "License plate is required")
    @Size(min = 4, max = 15, message = "License plate must be between 4 and 15 characters")
    private String licensePlate;

    @NotBlank(message = "Vehicle model is required")
    @Size(min = 2, max = 50, message = "Model must be between 2 and 50 characters")
    private String model;

    private VehicleStatus status;

    @NotNull(message = "Current mileage is required")
    @PositiveOrZero(message = "Mileage cannot be negative")
    @DecimalMax(value = "9999999.0", message = "Mileage cannot exceed 9,999,999")
    private Double currentMileage;

    public VehicleRequestDto(String vin, String licensePlate, String model, VehicleStatus status, Double currentMileage) {
        setVin(vin);
        setLicensePlate(licensePlate);
        setModel(model);
        this.status = status;
        this.currentMileage = currentMileage;
    }

    public void setVin(String vin) {
        this.vin = vin != null ? vin.trim().toUpperCase() : null;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate != null ? licensePlate.trim().toUpperCase() : null;
    }

    public void setModel(String model) {
        this.model = model != null ? model.trim() : null;
    }
}
