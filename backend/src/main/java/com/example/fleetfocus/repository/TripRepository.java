package com.example.fleetfocus.repository;

import com.example.fleetfocus.entity.Trip;
import com.example.fleetfocus.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByDriver_Username(String username);
    List<Trip> findByStatus(TripStatus status);
    List<Trip> findByDriver_UsernameAndStatus(String username, TripStatus status);
    long countByStatus(TripStatus status);
    long countByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}

