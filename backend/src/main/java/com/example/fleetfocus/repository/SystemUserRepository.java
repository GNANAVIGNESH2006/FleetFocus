package com.example.fleetfocus.repository;

import com.example.fleetfocus.entity.SystemUser;
import com.example.fleetfocus.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SystemUserRepository extends JpaRepository<SystemUser, Long>, JpaSpecificationExecutor<SystemUser> {
    Optional<SystemUser> findByUsername(String username);
    Optional<SystemUser> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    long countByRole(UserRole role);
}
