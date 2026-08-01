package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PantryItemResponse {
    private Long id;
    private Long userId;
    private String itemName;
    private String category;
    private BigDecimal quantity;
    private String unit;
    private LocalDate purchaseDate;
    private LocalDate expiryDate;
    private String barcode;
    private String storageLocation;
    private String status;
    private Integer daysUntilExpiry;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
