package com.manacommunity.api.payments.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Razorpay order endpoints.
 *
 * <p>{@link #keyId} is the Razorpay publishable key — safe to expose to the
 * frontend. It is included ONLY in the {@code POST /orders} response so the
 * frontend can open the checkout modal. All other responses return {@code null}
 * for this field.
 */
@Data
@Builder
public class PaymentOrderResponse {

    private Long id;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String currency;

    /** CREATED | PAID | FAILED | REFUNDED */
    private String status;

    private String referenceType;
    private Long referenceId;
    private String description;

    /**
     * Razorpay publishable key (safe to expose).
     * Present only in the order-creation response; null everywhere else.
     */
    private String keyId;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
