package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SocietyDueRequest(
    @NotBlank String flatNumber,
    Long residentUserId,
    @NotNull @Positive BigDecimal amount,
    @NotNull LocalDate dueDate,
    @NotNull String category,
    String period
) {}
