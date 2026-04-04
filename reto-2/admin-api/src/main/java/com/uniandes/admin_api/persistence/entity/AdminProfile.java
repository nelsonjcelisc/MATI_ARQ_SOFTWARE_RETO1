package com.uniandes.admin_api.persistence.entity;

import com.uniandes.admin_api.domain.model.AdminProfileStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admin_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false, unique = true)
    private String adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminProfileStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "adminProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AdminTrustedContext> trustedContexts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
