package com.example.fleetfocus.dto;

import com.example.fleetfocus.entity.Vehicle;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleResponseDto {

    private Long id;
    private String vin;
    private String licensePlate;
    private String model;
    private String status;
    private Double currentMileage;

    public static VehicleResponseDto fromEntity(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehicleResponseDto(
                vehicle.getId(),
                vehicle.getVin(),
                vehicle.getLicensePlate(),
                vehicle.getModel(),
                vehicle.getStatus() != null ? vehicle.getStatus().name() : null,
                vehicle.getCurrentMileage()
        );
    }

    public static VehicleResponseDto summary(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return summary(vehicle.getId(), vehicle.getLicensePlate(), vehicle.getModel());
    }

    public static VehicleResponseDto summary(Long id, String licensePlate, String model) {
        VehicleResponseDto dto = new VehicleResponseDto();
        dto.setId(id);
        dto.setLicensePlate(licensePlate);
        dto.setModel(model);
        return dto;
    }
}
