package com.example.fleetfocus.dto;

import com.example.fleetfocus.entity.Driver;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponseDto {

    private Long id;
    private String name;
    private String licenseNumber;
    private String status;
    private String username;

    public static DriverResponseDto fromEntity(Driver driver) {
        if (driver == null) {
            return null;
        }
        return new DriverResponseDto(
                driver.getId(),
                driver.getName(),
                driver.getLicenseNumber(),
                driver.getStatus() != null ? driver.getStatus().name() : null,
                driver.getUsername()
        );
    }
}
