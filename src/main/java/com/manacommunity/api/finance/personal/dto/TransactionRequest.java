package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull Long accountId,
        @NotNull String txnType,
        @NotNull @Positive BigDecimal amount,
        Long categoryId,
        @NotNull LocalDate txnDate,
        String description,
        String payee,
        Long transferToAccountId,
        String notes
) {}
