package com.uniandes.admin_api.persistence.mapper;

import com.uniandes.admin_api.domain.model.AdminProfileDTO;
import com.uniandes.admin_api.persistence.entity.AdminProfile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class AdminProfileMapper {

    private final AdminTrustedContextMapper trustedContextMapper;

    public AdminProfileMapper(AdminTrustedContextMapper trustedContextMapper) {
        this.trustedContextMapper = trustedContextMapper;
    }

    public AdminProfileDTO toDTO(AdminProfile entity) {
        if (entity == null) {
            return null;
        }
        return AdminProfileDTO.builder()
                .id(entity.getId())
                .adminId(entity.getAdminId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .trustedContexts(entity.getTrustedContexts() != null
                        ? entity.getTrustedContexts().stream()
                                .map(trustedContextMapper::toDTO)
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    public AdminProfile toEntity(AdminProfileDTO dto) {
        if (dto == null) {
            return null;
        }
        return AdminProfile.builder()
                .id(dto.getId())
                .adminId(dto.getAdminId())
                .status(dto.getStatus())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDTO(AdminProfile entity, AdminProfileDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setAdminId(dto.getAdminId());
        entity.setStatus(dto.getStatus());
    }
}
