package com.manacommunity.api.healthguard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "health_data_consent", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "provider", "data_scope", "consent_version"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthDataConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(nullable = false, length = 32)
    private String dataScope;

    @Column(nullable = false, length = 40)
    private String consentVersion;

    @Column(nullable = false)
    private OffsetDateTime grantedAt;

    private OffsetDateTime revokedAt;

    @Column(updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
