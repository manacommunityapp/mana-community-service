package com.manacommunity.api.finance.personal.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record RecurringTxnResponse(
        Long id,
        Long accountId,
        String accountName,
        String txnType,
        BigDecimal amount,
        Long categoryId,
        String categoryName,
        String description,
        String payee,
        String frequency,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextDueDate,
        boolean active,
        LocalDateTime createdAt
) {}
