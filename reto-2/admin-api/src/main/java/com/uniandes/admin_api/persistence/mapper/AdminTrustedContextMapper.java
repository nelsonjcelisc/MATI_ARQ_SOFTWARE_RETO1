package com.uniandes.admin_api.persistence.mapper;

import com.uniandes.admin_api.domain.model.AdminTrustedContextDTO;
import com.uniandes.admin_api.persistence.entity.AdminProfile;
import com.uniandes.admin_api.persistence.entity.AdminTrustedContext;
import org.springframework.stereotype.Component;

@Component
public class AdminTrustedContextMapper {

    public AdminTrustedContextDTO toDTO(AdminTrustedContext entity) {
        if (entity == null) {
            return null;
        }
        return AdminTrustedContextDTO.builder()
                .id(entity.getId())
                .adminProfileId(entity.getAdminProfile() != null
                        ? entity.getAdminProfile().getId()
                        : null)
                .type(entity.getType())
                .value(entity.getValue())
                .registeredAt(entity.getRegisteredAt())
                .build();
    }

    public AdminTrustedContext toEntity(AdminTrustedContextDTO dto) {
        if (dto == null) {
            return null;
        }
        return AdminTrustedContext.builder()
                .id(dto.getId())
                .type(dto.getType())
                .value(dto.getValue())
                .registeredAt(dto.getRegisteredAt())
                .build();
    }

    public AdminTrustedContext toEntity(AdminTrustedContextDTO dto, AdminProfile adminProfile) {
        if (dto == null) {
            return null;
        }
        return AdminTrustedContext.builder()
                .id(dto.getId())
                .adminProfile(adminProfile)
                .type(dto.getType())
                .value(dto.getValue())
                .registeredAt(dto.getRegisteredAt())
                .build();
    }

    public void updateEntityFromDTO(AdminTrustedContext entity, AdminTrustedContextDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setType(dto.getType());
        entity.setValue(dto.getValue());
    }
}
