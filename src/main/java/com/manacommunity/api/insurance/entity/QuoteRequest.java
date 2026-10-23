package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_quote_request")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuoteRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "community_id")
    private Long communityId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InsuranceEnums.Category category;
    @Column(name = "partner_id")
    private Long partnerId;
    @Column(name = "consent_id", nullable = false)
    private Long consentId;
    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InsuranceEnums.QuoteRequestStatus status;
    @Column(name = "failure_code", length = 40)
    private String failureCode;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
