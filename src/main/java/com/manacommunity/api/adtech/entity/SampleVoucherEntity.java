package com.manacommunity.api.adtech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "sample_vouchers", schema = "adtech")
@Getter
@Setter
@NoArgsConstructor
public class SampleVoucherEntity {

    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId = 0L;

    @Column(name = "campaign_id", nullable = false, length = 100)
    private String campaignId;

    @Column(name = "campaign_name", nullable = false)
    private String campaignName;

    @Column(name = "brand_name", nullable = false)
    private String brandName;

    @Column(name = "brand_logo_url", length = 1000)
    private String brandLogoUrl;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_image_url", length = 1000)
    private String productImageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "society_id", nullable = false, length = 100)
    private String societyId;

    @Column(name = "kiosk_location")
    private String kioskLocation;

    @Column(name = "kiosk_open_from", length = 50)
    private String kioskOpenFrom;

    @Column(name = "kiosk_open_until", length = 50)
    private String kioskOpenUntil;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "max_claims")
    private Integer maxClaims;

    @Column(name = "hmac_key_encrypted", columnDefinition = "TEXT", nullable = false)
    private String hmacKeyEncrypted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
