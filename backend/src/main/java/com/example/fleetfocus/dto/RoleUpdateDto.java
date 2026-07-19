package com.example.fleetfocus.dto;

import jakarta.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateDto {

    @Pattern(
            regexp = "ADMIN|DISPATCHER|DRIVER",
            message = "Role must be ADMIN, DISPATCHER or DRIVER"
    )
    private String role;
}

