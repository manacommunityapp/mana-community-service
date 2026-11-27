package com.manacommunity.api.db.optimization.wallet.model;

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
public class WalletLedgerEntryDto {

    private Long ledgerId;
    private Long walletId;
    private BigDecimal amount;
    private String transactionType; // MILK_SUBSCRIPTION, CLUBHOUSE_BOOKING, PREPAID_ELECTRICITY
    private BigDecimal balanceAfter;
    private String referenceId;
    private LocalDateTime timestamp;
}
