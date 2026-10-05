package com.manacommunity.api.commerce.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_refunds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_number", nullable = false, unique = true, length = 100)
    private String refundNumber;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number", nullable = false, length = 100)
    private String orderNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 50)
    private String status; // INITIATED, PROCESSED, FAILED

    @Column(name = "bank_reference", length = 100)
    private String bankReference;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
