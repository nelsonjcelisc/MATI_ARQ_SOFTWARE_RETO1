package com.uniandes.admin_api.persistence.impl;

import com.uniandes.admin_api.domain.model.AdminContextEventDTO;
import com.uniandes.admin_api.domain.model.Decision;
import com.uniandes.admin_api.domain.repository.AdminContextEventRepository;
import com.uniandes.admin_api.persistence.crud.AdminContextEventCRUD;
import com.uniandes.admin_api.persistence.entity.AdminContextEvent;
import com.uniandes.admin_api.persistence.mapper.AdminContextEventMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of AdminContextEventRepository.
 *
 * Bridges the domain layer with the persistence layer,
 * handling entity-DTO mapping and delegating to JPA CRUD.
 */
@Repository
public class AdminContextEventRepositoryImpl implements AdminContextEventRepository {

    private final AdminContextEventCRUD crudRepository;
    private final AdminContextEventMapper mapper;

    public AdminContextEventRepositoryImpl(
            AdminContextEventCRUD crudRepository,
            AdminContextEventMapper mapper) {
        this.crudRepository = crudRepository;
        this.mapper = mapper;
    }

    @Override
    public AdminContextEventDTO save(AdminContextEventDTO dto) {
        AdminContextEvent entity = mapper.toEntity(dto);
        AdminContextEvent saved = crudRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public List<AdminContextEventDTO> findByAdminId(String adminId) {
        return crudRepository.findByAdminIdOrderByTimestampDesc(adminId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<AdminContextEventDTO> findByDecision(Decision decision) {
        return crudRepository.findByDecisionOrderByTimestampDesc(decision).stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<AdminContextEventDTO> findByAdminIdAndTimeRange(
            String adminId,
            LocalDateTime start,
            LocalDateTime end) {
        return crudRepository.findByAdminIdAndTimestampBetween(adminId, start, end).stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<AdminContextEventDTO> findByDecisionSince(Decision decision, LocalDateTime since) {
        return crudRepository.findByDecisionAndTimestampAfter(decision, since).stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public long countByAdminIdAndDecision(String adminId, Decision decision) {
        return crudRepository.countByAdminIdAndDecision(adminId, decision);
    }
}
