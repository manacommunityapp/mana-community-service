package com.manacommunity.api.adtech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ad_impressions", schema = "adtech")
@Getter
@Setter
@NoArgsConstructor
public class AdImpressionEntity {

    @Id
    @Column(name = "impression_id", nullable = false)
    private UUID impressionId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "society_id", nullable = false, length = 100)
    private String societyId;

    @Column(name = "placement_id", nullable = false, length = 100)
    private String placementId;

    @Column(name = "cost_paise", nullable = false)
    private long costPaise = 0L;

    @Column(name = "fired_at", nullable = false)
    private Instant firedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
