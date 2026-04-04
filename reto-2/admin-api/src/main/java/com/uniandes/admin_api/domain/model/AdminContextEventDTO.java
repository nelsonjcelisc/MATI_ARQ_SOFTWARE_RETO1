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
public class AdminContextEventDTO {

    private Long id;
    private String adminId;
    private String device;
    private String ip;
    private Integer hour;
    private String agent;
    private Double score;
    private Decision decision;
    private LocalDateTime timestamp;
}
