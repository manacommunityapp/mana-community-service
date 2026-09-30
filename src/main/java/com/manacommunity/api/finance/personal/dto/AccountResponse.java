package com.manacommunity.api.finance.personal.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record AccountResponse(
        Long id,
        String accountName,
        String accountType,
        BigDecimal balance,
        String currency,
        String institution,
        String accountNumberMasked,
        String color,
        String icon,
        boolean active,
        boolean includeInTotal,
        LocalDateTime createdAt
) {}
