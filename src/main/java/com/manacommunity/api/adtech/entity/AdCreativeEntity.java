package com.manacommunity.api.adtech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ad_creatives", schema = "adtech")
@Getter
@Setter
@NoArgsConstructor
public class AdCreativeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creative_id", nullable = false, unique = true)
    private UUID creativeId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "creative_type", nullable = false, length = 30)
    private String creativeType;

    @Column(name = "asset_url", nullable = false, length = 1000)
    private String assetUrl;

    @Column(name = "cta_url", nullable = false, length = 1000)
    private String ctaUrl;

    @Column(name = "cta_text", length = 100)
    private String ctaText;

    @Column(name = "headline", length = 300)
    private String headline;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "brand_logo_url", length = 1000)
    private String brandLogoUrl;

    @Column(name = "weight", nullable = false)
    private int weight = 100;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
