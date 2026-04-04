package com.uniandes.admin_api.persistence.entity;

import com.uniandes.admin_api.domain.model.Decision;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_context_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContextEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private String adminId;

    @Column
    private String device;

    @Column
    private String ip;

    @Column
    private Integer hour;

    @Column
    private String agent;

    @Column(nullable = false)
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decision decision;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
