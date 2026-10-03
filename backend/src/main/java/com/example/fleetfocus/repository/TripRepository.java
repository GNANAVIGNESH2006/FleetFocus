package com.example.fleetfocus.repository;

import com.example.fleetfocus.entity.Trip;
import com.example.fleetfocus.entity.TripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {
    List<Trip> findByDriver_Username(String username);
    List<Trip> findByStatus(TripStatus status);
    List<Trip> findByDriver_UsernameAndStatus(String username, TripStatus status);

    long countByStatus(TripStatus status);
    long countByDriver_UsernameAndStatus(String username, TripStatus status);
    long countByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    long countByStatusAndStartTimeBefore(TripStatus status, LocalDateTime threshold);

    boolean existsByVehicle_Id(Long vehicleId);
    boolean existsByDriver_Id(Long driverId);
    boolean existsByVehicle_IdAndStatus(Long vehicleId, TripStatus status);
    boolean existsByDriver_IdAndStatus(Long driverId, TripStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);
}
