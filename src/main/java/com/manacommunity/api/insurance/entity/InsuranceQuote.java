package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_quote")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InsuranceQuote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "quote_request_id", nullable = false)
    private Long quoteRequestId;
    @Column(name = "partner_id")
    private Long partnerId;
    @Column(name = "product_version_id")
    private Long productVersionId;
    @Column(name = "partner_quote_ref", length = 80)
    private String partnerQuoteRef;
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private InsuranceEnums.QuoteSource dataSource;
    @Column(name = "premium_amount", nullable = false)
    private BigDecimal premiumAmount;
    @Column(name = "premium_frequency", nullable = false, length = 20)
    private String premiumFrequency;
    @Column(name = "sum_insured")
    private BigDecimal sumInsured;
    @Column(name = "quoted_at", nullable = false)
    private LocalDateTime quotedAt;
    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    public boolean isExpired(LocalDateTime now) {
        return !validUntil.isAfter(now);
    }
}
