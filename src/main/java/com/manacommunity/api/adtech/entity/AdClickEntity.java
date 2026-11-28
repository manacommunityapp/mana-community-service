package com.manacommunity.api.adtech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ad_clicks", schema = "adtech")
@Getter
@Setter
@NoArgsConstructor
public class AdClickEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "impression_id", nullable = false)
    private UUID impressionId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt = Instant.now();
}
