package com.example.fleetfocus.repository;

import com.example.fleetfocus.entity.Driver;
import com.example.fleetfocus.entity.DriverStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long>, JpaSpecificationExecutor<Driver> {
    List<Driver> findByStatus(DriverStatus status);
    long countByStatus(DriverStatus status);

    Optional<Driver> findByUsername(String username);
    Optional<Driver> findByUsernameIgnoreCase(String username);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);
    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Driver d where d.id = :id")
    Optional<Driver> findByIdForUpdate(@Param("id") Long id);
}
