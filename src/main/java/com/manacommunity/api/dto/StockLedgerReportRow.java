package com.manacommunity.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockLedgerReportRow(
    Long id,
    Long productId,
    String productName,
    String sku,
    Long locationId,
    String locationName,
    String lotName,
    String movementType,
    String referenceType,
    String referenceId,
    BigDecimal quantityChange,
    BigDecimal resultingBalance,
    BigDecimal unitCost,
    BigDecimal totalValue,
    LocalDateTime createdAt
) {}
