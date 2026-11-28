package com.manacommunity.api.payments.reconciliation.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Inbound payment gateway webhook payload (Razorpay / Cashfree / BillDesk / PayU).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookPayload {
    private String eventId;
    private String eventType; // "payment.captured", "payment.failed"
    private String paymentId; // Gateway transaction ID (e.g., pay_O8hK29)
    private String orderId;
    private String invoiceId;
    private String flatNumber;
    private String residentId;
    private String paymentMethod; // "UPI", "NETBANKING", "CARD", "NACH"
    private BigDecimal amount;
    private String currency;
    private String utr;
    private Instant gatewayTimestamp;
}
