package com.example.fleetfocus.dto;

import com.example.fleetfocus.entity.MaintenanceLog;
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
public class MaintenanceResponseDto {

    private Long id;
    private VehicleResponseDto vehicle;
    private LocalDate serviceDate;
    private String description;
    private BigDecimal cost;
    private String status;
    private LocalDate completedDate;

    public static MaintenanceResponseDto fromEntity(MaintenanceLog log) {
        if (log == null) {
            return null;
        }
        return new MaintenanceResponseDto(
                log.getId(),
                VehicleResponseDto.fromEntity(log.getVehicle()),
                log.getServiceDate(),
                log.getDescription(),
                log.getCost(),
                log.getStatus() != null ? log.getStatus().name() : null,
                log.getCompletedDate()
        );
    }
}
