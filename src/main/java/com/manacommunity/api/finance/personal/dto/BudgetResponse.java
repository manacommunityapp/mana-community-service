package com.manacommunity.api.finance.personal.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record BudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        String period,
        int budgetYear,
        Integer budgetMonth,
        int alertThresholdPct,
        LocalDateTime createdAt
) {}
