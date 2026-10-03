package com.example.fleetfocus.dto;

import com.example.fleetfocus.entity.Trip;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripResponseDto {

    private Long id;
    private VehicleResponseDto vehicle;
    private DriverResponseDto driver;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Double distanceCovered;

    public static TripResponseDto fromEntity(Trip trip) {
        if (trip == null) {
            return null;
        }
        return new TripResponseDto(
                trip.getId(),
                VehicleResponseDto.fromEntity(trip.getVehicle()),
                DriverResponseDto.fromEntity(trip.getDriver()),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getStatus() != null ? trip.getStatus().name() : null,
                trip.getDistanceCovered()
        );
    }
}
