package com.manacommunity.api.dto;

import java.math.BigDecimal;

public record ReorderAlertRow(
    Long productId,
    String productName,
    String defaultCode,
    BigDecimal currentOnHand,
    BigDecimal safetyStock,
    BigDecimal reorderQuantity,
    BigDecimal deficit
) {}
