package com.manacommunity.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockAgingReportRow(
    Long productId,
    String productName,
    String defaultCode,
    String locationName,
    BigDecimal quantityOnHand,
    long ageDays,
    String agingBucket,
    BigDecimal unitCost,
    BigDecimal totalValue,
    LocalDateTime lastMovementDate
) {}
