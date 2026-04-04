package com.uniandes.admin_api.web.mapper;

import com.uniandes.admin_api.domain.model.VendorCommissionDTO;
import com.uniandes.admin_api.web.dto.VendorCommissionRequest;
import com.uniandes.admin_api.web.dto.VendorCommissionResponse;
import org.springframework.stereotype.Component;

@Component
public class VendorCommissionWebMapper {

    public VendorCommissionDTO fromRequest(VendorCommissionRequest request) {
        if (request == null) {
            return null;
        }
        return VendorCommissionDTO.builder()
                .vendorId(request.getVendorId())
                .commissionRate(request.getCommissionRate())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .createdBy(request.getCreatedBy())
                .active(request.getActive())
                .build();
    }

    public VendorCommissionResponse toResponse(VendorCommissionDTO dto) {
        if (dto == null) {
            return null;
        }
        return VendorCommissionResponse.builder()
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
}
