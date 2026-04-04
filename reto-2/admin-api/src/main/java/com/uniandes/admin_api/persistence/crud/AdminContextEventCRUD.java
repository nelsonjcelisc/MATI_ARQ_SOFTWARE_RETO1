package com.uniandes.admin_api.persistence.crud;

import com.uniandes.admin_api.domain.model.Decision;
import com.uniandes.admin_api.persistence.entity.AdminContextEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository for AdminContextEvent entity.
 *
 * Provides persistence operations for security context events,
 * including both allowed and blocked access attempts.
 */
@Repository
public interface AdminContextEventCRUD extends JpaRepository<AdminContextEvent, Long> {

    /**
     * Find all events for a specific admin.
     */
    List<AdminContextEvent> findByAdminIdOrderByTimestampDesc(String adminId);

    /**
     * Find events by decision type (ALLOWED or BLOCKED).
     */
    List<AdminContextEvent> findByDecisionOrderByTimestampDesc(Decision decision);

    /**
     * Find events for a specific admin within a time range.
     */
    List<AdminContextEvent> findByAdminIdAndTimestampBetween(
            String adminId,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Find blocked events within a time range (for security monitoring).
     */
    List<AdminContextEvent> findByDecisionAndTimestampAfter(
            Decision decision,
            LocalDateTime since
    );

    /**
     * Count blocked attempts for an admin (useful for rate limiting or escalation).
     */
    long countByAdminIdAndDecision(String adminId, Decision decision);
}
