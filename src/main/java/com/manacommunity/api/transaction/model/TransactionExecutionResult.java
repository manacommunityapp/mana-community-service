package com.manacommunity.api.transaction.model;

import com.manacommunity.api.transaction.enums.TransactionDomain;
import com.manacommunity.api.transaction.enums.TransactionPaymentMethod;
import com.manacommunity.api.transaction.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionExecutionResult {
    private Long id;
    private String transactionNumber;
    private TransactionDomain domain;
    private TransactionStatus status;
    private TransactionPaymentMethod paymentMethod;
    private BigDecimal amount;
    private BigDecimal netAmount;
    private BigDecimal taxAmount;
    private BigDecimal platformFee;
    private BigDecimal vendorPayout;

    private Long payerId;
    private Long payeeId;

    private Long paymentId;
    private Long invoiceId;
    private Long receiptId;
    private Long journalEntryId;
    private Long settlementId;
    private Long refundId;

    private boolean isEscrow;
    private boolean escrowReleased;

    private String gatewayReference;
    private String narration;
    private LocalDateTime createdAt;
}
