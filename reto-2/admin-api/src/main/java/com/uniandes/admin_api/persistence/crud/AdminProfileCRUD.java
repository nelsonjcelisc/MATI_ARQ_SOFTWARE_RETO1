package com.uniandes.admin_api.persistence.crud;

import com.uniandes.admin_api.domain.model.AdminProfileStatus;
import com.uniandes.admin_api.persistence.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminProfileCRUD extends JpaRepository<AdminProfile, Long> {

    // Simple queries (without trustedContexts)
    List<AdminProfile> findByStatus(AdminProfileStatus status);
    Optional<AdminProfile> findByAdminId(String adminId);

    // Queries with JOIN FETCH to eagerly load trustedContexts
    @Query("SELECT DISTINCT p FROM AdminProfile p LEFT JOIN FETCH p.trustedContexts WHERE p.status = :status")
    List<AdminProfile> findByStatusWithTrustedContexts(@Param("status") AdminProfileStatus status);

    @Query("SELECT p FROM AdminProfile p LEFT JOIN FETCH p.trustedContexts WHERE p.adminId = :adminId")
    Optional<AdminProfile> findByAdminIdWithTrustedContexts(@Param("adminId") String adminId);

    @Query("SELECT DISTINCT p FROM AdminProfile p LEFT JOIN FETCH p.trustedContexts")
    List<AdminProfile> findAllWithTrustedContexts();
}
