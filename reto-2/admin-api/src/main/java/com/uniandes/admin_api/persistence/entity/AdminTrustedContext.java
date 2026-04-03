package com.uniandes.admin_api.persistence.entity;

import com.uniandes.admin_api.domain.model.ContextType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_trusted_contexts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTrustedContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_profile_id", nullable = false)
    private AdminProfile adminProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContextType type;

    @Column(nullable = false)
    private String value;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
    }
}
