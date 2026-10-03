package com.example.fleetfocus.dto;

import com.example.fleetfocus.entity.UserRole;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateDto {

    @NotNull(message = "Role is required")
    private UserRole role;
}

