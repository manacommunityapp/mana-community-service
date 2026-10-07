package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentRequest {
    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // UPI, CARD, NET_BANKING, WALLET, COD

    private String transactionId;
    private BigDecimal amountPaid;
    private String paymentGateway; // RAZORPAY, STRIPE, UPI_INTENT, SIMULATED
}
