package com.manacommunity.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FefoRecommendationRow(
    Long lotId,
    String lotName,
    Long productId,
    String productName,
    String defaultCode,
    String locationName,
    BigDecimal availableQuantity,
    LocalDate expirationDate,
    long daysUntilExpiry,
    String urgency
) {}
