package com.manacommunity.api.finance.personal.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record FinanceSummaryResponse(
        BigDecimal totalBalance,
        BigDecimal monthIncome,
        BigDecimal monthExpenses,
        BigDecimal monthSavings,
        int totalAccounts,
        int pendingBills,
        BigDecimal pendingBillsAmount,
        List<CategoryBreakdown> topExpenseCategories
) {
    @Builder
    public record CategoryBreakdown(
            String categoryName,
            BigDecimal amount,
            double percentage
    ) {}
}
