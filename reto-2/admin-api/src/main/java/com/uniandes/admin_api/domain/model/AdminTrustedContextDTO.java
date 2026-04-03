package com.uniandes.admin_api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTrustedContextDTO {

    private Long id;
    private Long adminProfileId;
    private ContextType type;
    private String value;
    private LocalDateTime registeredAt;
}
