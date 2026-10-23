package com.manacommunity.api.payments.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregate payment statistics for the admin report dashboard.
 */
@Data
@Builder
public class CashfreeAdminSummaryResponse {

    /** Total amount collected from PAID orders in the period. */
    private BigDecimal totalCollected;

    /** Total amount refunded (REFUNDED + REFUND_PENDING) in the period. */
    private BigDecimal totalRefunded;

    /** Count of orders per status, e.g. {"PAID": 42, "FAILED": 5}. */
    private Map<String, Long> countByStatus;

    /** Sum of PAID amounts per status, e.g. {"PAID": 12500.00}. */
    private Map<String, BigDecimal> amountByStatus;

    /** Count of PAID orders per referenceType, e.g. {"SPORTS_REGISTRATION": 30}. */
    private Map<String, Long> countByReferenceType;

    /** Sum of PAID amounts per referenceType. */
    private Map<String, BigDecimal> revenueByReferenceType;
}
