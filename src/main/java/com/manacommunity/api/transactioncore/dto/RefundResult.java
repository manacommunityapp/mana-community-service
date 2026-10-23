package com.manacommunity.api.transactioncore.dto;

import com.manacommunity.api.transactioncore.enums.RefundDestination;
import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
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
public class RefundResult {

    private String refundId;
    private String intentId;
    private BigDecimal amountRefunded;
    private RefundDestination destination;
    private TransactionIntentStatus status;
    private String journalEntryNumber;
    private String gatewayRefundId;
    private LocalDateTime refundedAt;
    private String message;
}
