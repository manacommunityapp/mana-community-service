package com.manacommunity.api.commerce.core.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "commerce_dispute")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceDispute extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_code", unique = true, nullable = false, length = 64)
    private String disputeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CommerceOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommerceChannel channel;

    @Column(nullable = false, length = 64)
    private String reason;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "requested_resolution", length = 64, nullable = false)
    private String requestedResolution;

    @Column(name = "claim_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal claimAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "SUBMITTED";

    @Column(name = "vendor_response", columnDefinition = "TEXT")
    private String vendorResponse;

    @Column(name = "evidence_urls_json", columnDefinition = "TEXT")
    private String evidenceUrlsJson;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}