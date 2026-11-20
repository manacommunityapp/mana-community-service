package com.manacommunity.api.payments.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_mandate")
@Data
public class PaymentMandate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "mandate_type", nullable = false, length = 100) private String mandateType;
    @Column(nullable = false, length = 200) private String provider;
    @Column(name = "max_amount", nullable = false, precision = 12, scale = 2) private BigDecimal maxAmount;
    @Column(nullable = false, length = 50) private String frequency;
    @Column(name = "approved_at") private Instant approvedAt = Instant.now();
    @Column(name = "expires_at") private Instant expiresAt;
    @Column private Boolean active = true;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "cancel_reason", length = 500) private String cancelReason;
}
