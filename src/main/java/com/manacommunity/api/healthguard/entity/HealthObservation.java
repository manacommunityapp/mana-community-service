package com.manacommunity.api.healthguard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "health_observation", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "provider", "source_record_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(nullable = false, length = 160)
    private String sourceRecordId;

    @Column(length = 160)
    private String sourceApp;

    @Column(nullable = false, length = 32)
    private String observationType;

    @Column(precision = 20, scale = 6)
    private BigDecimal numericValue;

    @Column(length = 32)
    private String unit;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    @Column(nullable = false)
    private OffsetDateTime endedAt;

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
