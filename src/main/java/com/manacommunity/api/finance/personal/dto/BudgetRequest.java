package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotNull Long categoryId,
        @NotNull @Positive BigDecimal budgetAmount,
        String period,
        @NotNull Integer budgetYear,
        Integer budgetMonth,
        Integer alertThresholdPct
) {}
