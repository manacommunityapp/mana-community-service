package com.manacommunity.api.payments.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "payment_reconciliation",
    uniqueConstraints = @UniqueConstraint(columnNames = {"community_id", "period_start", "period_end"}))
@Data
public class PaymentReconciliation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end", nullable = false) private LocalDate periodEnd;
    @Column(name = "total_collected", precision = 12, scale = 2) private BigDecimal totalCollected = BigDecimal.ZERO;
    @Column(name = "total_refunded", precision = 12, scale = 2) private BigDecimal totalRefunded = BigDecimal.ZERO;
    @Column(name = "total_pending", precision = 12, scale = 2) private BigDecimal totalPending = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal discrepancy = BigDecimal.ZERO;
    @Column(nullable = false, length = 50) private String status = "DRAFT";
    @Column(name = "reviewed_by") private Long reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(length = 1000) private String notes;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
