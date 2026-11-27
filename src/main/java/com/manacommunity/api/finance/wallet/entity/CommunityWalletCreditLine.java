package com.manacommunity.api.finance.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "community_wallet_credit_lines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunityWalletCreditLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "approved_by", nullable = false)
    private Long approvedBy;

    @Column(name = "approved_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "interest_rate_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal interestRatePct = BigDecimal.ZERO;

    @Column(name = "repayment_days", nullable = false)
    @Builder.Default
    private Integer repaymentDays = 90;

    @Column(name = "drawn_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal drawnAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "approved_at", nullable = false)
    @Builder.Default
    private OffsetDateTime approvedAt = OffsetDateTime.now();

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
}
