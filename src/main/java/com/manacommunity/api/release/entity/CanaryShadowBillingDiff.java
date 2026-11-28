package com.manacommunity.api.release.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "canary_shadow_billing_diffs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CanaryShadowBillingDiff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_key", nullable = false, length = 100)
    private String flagKey;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "invoice_ref", nullable = false, length = 64)
    private String invoiceRef;

    @Column(name = "legacy_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal legacyTotal;

    @Column(name = "canary_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal canaryTotal;

    @Column(name = "delta_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal deltaAmount;

    @Column(nullable = false)
    private boolean matched;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
