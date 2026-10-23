package com.manacommunity.api.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyDashboardSummaryDto {
    private BigDecimal operatingBalance;
    private BigDecimal sinkingFundBalance;
    private BigDecimal fixedDepositsBalance;
    private BigDecimal totalReserves;
    private BigDecimal totalMonthlyDemand;
    private BigDecimal totalCollected;
    private BigDecimal collectionRate;
    private BigDecimal outstandingReceivables;
    private BigDecimal pendingPayables;
    private long pendingApprovalsCount;
    private long recentTransactionsCount;
}
