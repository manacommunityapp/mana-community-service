package com.manacommunity.api.finance.personal.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record TransactionResponse(
        Long id,
        Long accountId,
        String accountName,
        String txnType,
        BigDecimal amount,
        Long categoryId,
        String categoryName,
        LocalDate txnDate,
        String description,
        String payee,
        Long transferToAccountId,
        String transferToAccountName,
        String sourceModule,
        Long sourceRefId,
        String notes,
        boolean recurringInstance,
        LocalDateTime createdAt
) {}
