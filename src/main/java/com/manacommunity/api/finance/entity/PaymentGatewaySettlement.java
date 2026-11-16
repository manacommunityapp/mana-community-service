package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pg_settlement", indexes = {
        @Index(name = "idx_pg_settlement_community", columnList = "community_id"),
        @Index(name = "idx_pg_settlement_date", columnList = "settlement_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewaySettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "utr_number", length = 60)
    private String utrNumber;

    @Column(name = "gross_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "mdr_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal mdrAmount = BigDecimal.ZERO;

    @Column(name = "gst_on_mdr", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal gstOnMdr = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, RECONCILED

    @Column(name = "transaction_count")
    @Builder.Default
    private Integer transactionCount = 0;

    @Column(name = "bank_account", length = 120)
    private String bankAccount;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
