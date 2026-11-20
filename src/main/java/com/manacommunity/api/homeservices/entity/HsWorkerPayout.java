package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "hs_worker_payout") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsWorkerPayout {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "worker_id", nullable = false) private Long workerId;
    @Column(name = "booking_id") private Long bookingId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal deductions = BigDecimal.ZERO;
    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2) private BigDecimal netAmount;
    @Column(name = "period_start") private LocalDate periodStart;
    @Column(name = "period_end") private LocalDate periodEnd;
    @Column(nullable = false) private Boolean paid = false;
    @Column(name = "paid_at") private Instant paidAt;
    private String notes;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
