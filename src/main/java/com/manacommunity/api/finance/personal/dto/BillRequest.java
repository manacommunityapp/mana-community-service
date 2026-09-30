package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillRequest(
        @NotBlank String name,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate dueDate,
        Long categoryId,
        Boolean recurring,
        String recurrencePeriod,
        Boolean autoPay,
        String notes
) {}
