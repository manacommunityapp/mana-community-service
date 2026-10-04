package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SocietyExpenseRequest(
    @NotBlank String title,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String category,
    String vendor,
    String approvedBy,
    @NotNull LocalDate date,
    String receiptUrl,
    String notes
) {}
