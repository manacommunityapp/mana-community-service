package com.manacommunity.api.dto;

import java.math.BigDecimal;

/**
 * Aggregated totals for the expenses dashboard — counts and summed amounts
 * per status, scoped to a single community.
 */
public record ExpenseSummaryResponse(
        long totalCount,
        BigDecimal totalAmount,
        long pendingCount,
        BigDecimal pendingAmount,
        long approvedCount,
        BigDecimal approvedAmount,
        long rejectedCount,
        BigDecimal rejectedAmount
) {}
