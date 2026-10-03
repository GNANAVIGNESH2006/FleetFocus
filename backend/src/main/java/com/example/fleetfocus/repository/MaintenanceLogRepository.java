package com.example.fleetfocus.repository;

import com.example.fleetfocus.entity.MaintenanceLog;
import com.example.fleetfocus.entity.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long>, JpaSpecificationExecutor<MaintenanceLog> {
    boolean existsByVehicle_Id(Long vehicleId);
    boolean existsByVehicle_IdAndStatus(Long vehicleId, MaintenanceStatus status);
    List<MaintenanceLog> findByVehicle_Id(Long vehicleId);
    List<MaintenanceLog> findByStatus(MaintenanceStatus status);
    List<MaintenanceLog> findByVehicle_IdAndStatus(Long vehicleId, MaintenanceStatus status);
}
