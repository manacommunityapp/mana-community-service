package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PersonalBudgetRequest(
    @NotBlank String category,
    @NotNull @Positive BigDecimal monthlyLimit,
    @NotNull Integer month,
    @NotNull Integer year
) {}
