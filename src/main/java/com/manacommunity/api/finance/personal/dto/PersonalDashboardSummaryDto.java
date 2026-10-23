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
    
    // Community Spending vs Other Personal Spending
    private BigDecimal totalCommunitySpending;
    private List<PersonalSpendingCategoryDto> communitySpendingBreakdown;
    private BigDecimal totalOtherSpending;
    private List<PersonalSpendingCategoryDto> otherSpendingBreakdown;

    private List<PersonalTransactionDto> recentTransactions;
    private List<PersonalTransactionDto> manaProjections;
    private List<PersonalBudgetDto> budgetAlerts;
}
