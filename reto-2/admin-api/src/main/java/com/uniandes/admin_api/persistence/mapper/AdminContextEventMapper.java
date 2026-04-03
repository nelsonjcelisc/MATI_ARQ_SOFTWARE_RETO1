package com.uniandes.admin_api.persistence.mapper;

import com.uniandes.admin_api.domain.model.AdminContextEventDTO;
import com.uniandes.admin_api.persistence.entity.AdminContextEvent;
import org.springframework.stereotype.Component;

@Component
public class AdminContextEventMapper {

    public AdminContextEventDTO toDTO(AdminContextEvent entity) {
        if (entity == null) {
            return null;
        }
        return AdminContextEventDTO.builder()
                .id(entity.getId())
                .adminId(entity.getAdminId())
                .device(entity.getDevice())
                .ip(entity.getIp())
                .hour(entity.getHour())
                .agent(entity.getAgent())
                .score(entity.getScore())
                .decision(entity.getDecision())
                .timestamp(entity.getTimestamp())
                .build();
    }

    public AdminContextEvent toEntity(AdminContextEventDTO dto) {
        if (dto == null) {
            return null;
        }
        return AdminContextEvent.builder()
                .id(dto.getId())
                .adminId(dto.getAdminId())
                .device(dto.getDevice())
                .ip(dto.getIp())
                .hour(dto.getHour())
                .agent(dto.getAgent())
                .score(dto.getScore())
                .decision(dto.getDecision())
                .timestamp(dto.getTimestamp())
                .build();
    }
}
