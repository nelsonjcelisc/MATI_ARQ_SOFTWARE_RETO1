package com.uniandes.admin_api.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorCommissionRequest {

    @NotBlank(message = "Vendor ID is required")
    private String vendorId;

    @NotNull(message = "Commission rate is required")
    @DecimalMin(value = "0.00", message = "Commission rate must be at least 0.00")
    @DecimalMax(value = "100.00", message = "Commission rate must be at most 100.00")
    private BigDecimal commissionRate;

    @NotNull(message = "Effective from date is required")
    private LocalDateTime effectiveFrom;

    private Boolean active = true;

    private LocalDateTime effectiveTo;

    @NotNull
    private String createdBy;
}
