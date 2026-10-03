package com.example.fleetfocus.repository;

import com.example.fleetfocus.entity.Vehicle;
import com.example.fleetfocus.entity.VehicleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {
    List<Vehicle> findByStatus(VehicleStatus status);
    long countByStatus(VehicleStatus status);

    boolean existsByVinIgnoreCase(String vin);
    boolean existsByVinIgnoreCaseAndIdNot(String vin, Long id);

    boolean existsByLicensePlateIgnoreCase(String licensePlate);
    boolean existsByLicensePlateIgnoreCaseAndIdNot(String licensePlate, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);
}
