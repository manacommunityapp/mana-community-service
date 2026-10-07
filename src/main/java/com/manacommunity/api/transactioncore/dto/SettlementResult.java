package com.manacommunity.api.transactioncore.dto;

import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResult {

    private String intentId;
    private String settlementBatchId;
    private TransactionIntentStatus status;
    private BigDecimal totalSettled;
    private List<PaymentSplit> splitsSettled;
    private String journalEntryNumber;
    private LocalDateTime settledAt;
    private String message;
}
