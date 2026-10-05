package com.manacommunity.api.commerce.core.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "commerce_payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommercePayment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CommerceOrder order;

    @Column(name = "payment_ref", unique = true, nullable = false, length = 80)
    private String paymentRef;

    @Column(length = 32, nullable = false)
    @Builder.Default
    private String gateway = "RAZORPAY";

    @Column(name = "payment_method", length = 32, nullable = false)
    private String paymentMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 8, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(length = 32, nullable = false)
    @Builder.Default
    private String status = "CAPTURED";

    @Column(name = "gateway_signature", columnDefinition = "TEXT")
    private String gatewaySignature;
}