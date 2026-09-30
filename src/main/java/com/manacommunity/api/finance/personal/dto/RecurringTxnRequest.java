package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTxnRequest(
        @NotNull Long accountId,
        @NotNull String txnType,
        @NotNull @Positive BigDecimal amount,
        Long categoryId,
        String description,
        String payee,
        @NotNull String frequency,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {}
