package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MeterReadingRequest(
    @NotNull @Positive BigDecimal readingValue,
    @NotNull LocalDate readingDate,
    BigDecimal consumption,
    String unit,
    BigDecimal billedAmount
) {}
