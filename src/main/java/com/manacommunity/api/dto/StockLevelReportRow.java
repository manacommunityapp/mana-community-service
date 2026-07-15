package com.manacommunity.api.dto;

import java.math.BigDecimal;

public record StockLevelReportRow(
    Long productId,
    String productName,
    String defaultCode,
    BigDecimal onHand,
    BigDecimal reserved,
    BigDecimal available,
    String locationName
) {}
