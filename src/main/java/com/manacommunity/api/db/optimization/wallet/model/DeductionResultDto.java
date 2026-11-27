package com.manacommunity.api.db.optimization.wallet.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductionResultDto {

    private boolean success;
    private Long walletId;
    private BigDecimal deductedAmount;
    private BigDecimal balanceAfter;
    private Long ledgerEntryId;
    private String transactionType;
    private String errorCode;
    private String errorMessage;
}
