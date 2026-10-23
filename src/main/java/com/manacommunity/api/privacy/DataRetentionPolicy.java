package com.manacommunity.api.privacy;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_retention_policy", indexes = {
    @Index(name = "idx_drp_community", columnList = "community_id"),
    @Index(name = "idx_drp_category",  columnList = "data_category")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataRetentionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "data_category", nullable = false, length = 60)
    private String dataCategory;

    @Column(name = "retention_period_days", nullable = false)
    private Integer retentionPeriodDays;

    @Column(name = "action_on_expiry", nullable = false, length = 30)
    @Builder.Default
    private String actionOnExpiry = "ANONYMIZE"; // DELETE, ANONYMIZE, ARCHIVE

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
