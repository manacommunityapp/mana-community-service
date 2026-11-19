package com.manacommunity.api.payments.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_receipt")
@Data
public class PaymentReceipt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "source_module", nullable = false, length = 100) private String sourceModule;
    @Column(name = "reference_id", nullable = false) private Long referenceId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 10) private String currency = "INR";
    @Column(name = "payment_method", length = 100) private String paymentMethod;
    @Column(name = "gateway_ref", length = 200) private String gatewayRef;
    @Column(name = "receipt_number", nullable = false, unique = true, length = 100) private String receiptNumber;
    @Column(name = "paid_at", nullable = false) private Instant paidAt;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
