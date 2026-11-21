package com.manacommunity.api.analytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "product_analytics_event",
    indexes = {
        @Index(name = "idx_pae_comm_occurred", columnList = "community_id, occurred_at"),
        @Index(name = "idx_pae_event_occurred", columnList = "event_name, occurred_at"),
        @Index(name = "idx_pae_cohort", columnList = "cohort")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "user_hash", length = 64)
    private String userHash;

    @Column(name = "cohort", length = 50)
    private String cohort;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "props_json", columnDefinition = "TEXT")
    private String propsJson;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (occurredAt == null) occurredAt = OffsetDateTime.now();
    }
}
