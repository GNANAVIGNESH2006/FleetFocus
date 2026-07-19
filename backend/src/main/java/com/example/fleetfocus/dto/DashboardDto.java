package com.example.fleetfocus.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DashboardDto {

    private String role;

    private Long totalVehicles;
    private Long availableVehicles;
    private Long vehiclesOnTrip;
    private Long vehiclesUnderMaintenance;
    private Long totalDrivers;
    private Long availableDrivers;
    private Long totalUsers;
    private Long totalTrips;
    private Long completedTrips;

    private Long todaysTrips;
    private Long activeTrips;
    private Long pendingAssignments;

    private String driverName;
    private String myVehiclePlate;
    private String myVehicleModel;
    private String myVehicleStatus;
    private Long myCurrentTripId;
    private String myCurrentTripStatus;
    private Long myCompletedTripsCount;
}

