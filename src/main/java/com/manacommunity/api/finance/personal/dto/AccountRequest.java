package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank String accountName,
        @NotNull String accountType,
        BigDecimal balance,
        String currency,
        String institution,
        String accountNumberMasked,
        String color,
        String icon,
        Boolean includeInTotal
) {}
