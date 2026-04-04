package com.uniandes.admin_api.persistence.mapper;

import com.uniandes.admin_api.domain.model.VendorCommissionDTO;
import com.uniandes.admin_api.persistence.entity.VendorCommission;
import org.springframework.stereotype.Component;

@Component
public class VendorCommissionMapper {

    public VendorCommissionDTO toDTO(VendorCommission entity) {
        if (entity == null) {
            return null;
        }
        return VendorCommissionDTO.builder()
                .id(entity.getId())
                .vendorId(entity.getVendorId())
                .commissionRate(entity.getCommissionRate())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public VendorCommission toEntity(VendorCommissionDTO dto) {
        if (dto == null) {
            return null;
        }
        return VendorCommission.builder()
                .id(dto.getId())
                .vendorId(dto.getVendorId())
                .commissionRate(dto.getCommissionRate())
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveTo(dto.getEffectiveTo())
                .active(dto.getActive())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .createdBy(dto.getCreatedBy())
                .build();
    }

    public void updateEntityFromDTO(VendorCommission entity, VendorCommissionDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setVendorId(dto.getVendorId());
        entity.setCommissionRate(dto.getCommissionRate());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        entity.setActive(dto.getActive());
    }
}
