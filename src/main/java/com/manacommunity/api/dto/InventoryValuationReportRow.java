package com.manacommunity.api.dto;

import java.math.BigDecimal;

public record InventoryValuationReportRow(
    Long productId,
    String productName,
    String defaultCode,
    String categoryName,
    Long warehouseId,
    String warehouseName,
    Long locationId,
    String locationName,
    BigDecimal quantityOnHand,
    BigDecimal unitCost,
    BigDecimal totalValuation
) {}
