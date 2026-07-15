package com.manacommunity.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InventoryLotResponse(
    Long id,
    String name,
    Long productId,
    String productName,
    LocalDate expirationDate,
    String notes,
    LocalDateTime createdAt
) {}
