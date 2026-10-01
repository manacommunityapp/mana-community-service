package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalDashboardSummaryDto {
    private String month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netSavings;
    private int savingsRate;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal netWorth;
    private List<PersonalTransactionDto> recentTransactions;
    private List<PersonalTransactionDto> manaProjections;
    private List<PersonalBudgetDto> budgetAlerts;
}
