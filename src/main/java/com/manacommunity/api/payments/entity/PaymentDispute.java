package com.manacommunity.api.payments.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_dispute")
@Data
public class PaymentDispute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "receipt_id") private Long receiptId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 500) private String reason;
    @Column(length = 2000) private String description;
    @Column(nullable = false, length = 50) private String status = "OPEN";
    @Column(name = "assigned_to") private Long assignedTo;
    @Column(length = 2000) private String resolution;
    @Column(name = "resolved_at") private Instant resolvedAt;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
