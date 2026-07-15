package com.manacommunity.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryScrapResponse(
    Long id,
    Long productId,
    String productName,
    Long lotId,
    String lotName,
    String sourceLocationName,
    String scrapLocationName,
    BigDecimal quantity,
    String reason,
    String state,
    String scrappedByName,
    LocalDateTime scrappedAt,
    LocalDateTime createdAt
) {}
