package com.example.fleetfocus.dto;

import com.example.fleetfocus.entity.DriverStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatusUpdateDto {

    @NotNull(message = "Status is required")
    private DriverStatus status;
}
