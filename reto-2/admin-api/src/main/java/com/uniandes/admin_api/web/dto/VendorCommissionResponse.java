package com.uniandes.admin_api.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorCommissionResponse {

    private Long id;
    private String vendorId;
    private BigDecimal commissionRate;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
