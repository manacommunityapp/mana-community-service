package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pilot_community_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PilotCommunityConfig {

    @Id
    @Column(name = "community_id")
    private Long communityId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "is_pilot", nullable = false)
    @Builder.Default
    private Boolean isPilot = false;

    @Column(name = "cohort", length = 50)
    private String cohort;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
        if (isPilot == null) isPilot = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
