package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalTransactionRequest(
    @NotNull String type,
    @NotBlank String category,
    @NotNull @Positive BigDecimal amount,
    String description,
    @NotNull LocalDate date,
    String paymentMethod,
    Boolean recurring,
    String recurringInterval,
    String attachmentUrl
) {}
