package com.manacommunity.api.payments.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_request_link")
@Data
public class PaymentRequestLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "payee_user_id") private Long payeeUserId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 500) private String description;
    @Column(name = "link_token", nullable = false, unique = true, length = 64) private String linkToken;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column private Boolean paid = false;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "paid_by") private Long paidBy;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
