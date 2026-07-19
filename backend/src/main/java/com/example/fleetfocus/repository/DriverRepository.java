package com.example.fleetfocus.repository;

import com.example.fleetfocus.entity.Driver;
import com.example.fleetfocus.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByStatus(DriverStatus status);
    Optional<Driver> findByUsername(String username);
}

