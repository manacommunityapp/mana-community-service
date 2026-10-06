package com.manacommunity.api.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRefundRequest {
    private String transactionNumber;
    private BigDecimal refundAmount;
    private String reason;
    private boolean refundToWallet;
}
