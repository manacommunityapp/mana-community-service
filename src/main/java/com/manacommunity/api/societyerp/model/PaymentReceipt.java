package com.manacommunity.api.societyerp.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Official digitally signed payment receipt for maintenance dues.
 * Contains cryptographic SHA-256 hash for offline audit verification.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReceipt {

    private String receiptNumber;
    private String invoiceId;
    private String societyId;
    private String societyName;
    private String societyGstin;

    private String flatNumber;
    private String residentName;
    private String billingPeriod;

    private BigDecimal totalBilledAmount;
    private BigDecimal advanceWalletDeducted;
    private BigDecimal netAmountPaid;

    private String paymentMethod; // "ADVANCE_WALLET", "UPI", "NETBANKING", "CARD"
    private String transactionRef;
    private String utr;
    private Instant paymentTimestamp;

    private String digitalVerificationHash;
    private String downloadUrl;
}
