package com.manacommunity.api.groupbuying.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentResponse {
    private String orderNumber;
    private String paymentStatus; // PAID, ESCROW_HELD, PENDING, FAILED
    private String paymentMethod;
    private String transactionId;
    private BigDecimal amountPaid;
    private BigDecimal escrowHoldAmount;
    private LocalDateTime paymentTimestamp;
    private String message;
}
