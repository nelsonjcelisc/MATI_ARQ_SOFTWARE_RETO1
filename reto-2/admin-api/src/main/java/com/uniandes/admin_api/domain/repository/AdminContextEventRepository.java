package com.uniandes.admin_api.domain.repository;

import com.uniandes.admin_api.domain.model.AdminContextEventDTO;
import com.uniandes.admin_api.domain.model.Decision;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AdminContextEvent operations.
 *
 * Follows the repository pattern to decouple domain logic
 * from persistence implementation details.
 */
public interface AdminContextEventRepository {

    /**
     * Save a new context event.
     */
    AdminContextEventDTO save(AdminContextEventDTO event);

    /**
     * Find all events for a specific admin, ordered by timestamp descending.
     */
    List<AdminContextEventDTO> findByAdminId(String adminId);

    /**
     * Find events by decision type (ALLOWED or BLOCKED).
     */
    List<AdminContextEventDTO> findByDecision(Decision decision);

    /**
     * Find events for a specific admin within a time range.
     */
    List<AdminContextEventDTO> findByAdminIdAndTimeRange(
            String adminId,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Find events by decision since a specific time.
     */
    List<AdminContextEventDTO> findByDecisionSince(Decision decision, LocalDateTime since);

    /**
     * Count events by admin and decision type.
     */
    long countByAdminIdAndDecision(String adminId, Decision decision);
}
