package com.manacommunity.api.payments.config.dto;

import com.manacommunity.api.payments.model.PaymentReferenceType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecordManualPaymentRequest {
    /** Community scope — validated against principal's community. */
    private Long communityId;
    private PaymentReferenceType referenceType;
    private Long referenceId;
    /** Amount in INR — NUMERIC, never float. Validated server-side against expected amount. */
    private BigDecimal amount;
    /** CASH, CHEQUE, BANK_TRANSFER, UPI_MANUAL */
    private String paymentMode;
    /** Cheque number, bank UTR, etc. — optional. */
    private String referenceNumber;
    /** Admin note for audit trail — optional. */
    private String remarks;
    /**
     * The resident/payer's user ID — the person who actually made the payment.
     * Optional: if omitted, the recording admin's userId is stored as a fallback.
     */
    private Long payerUserId;
}
