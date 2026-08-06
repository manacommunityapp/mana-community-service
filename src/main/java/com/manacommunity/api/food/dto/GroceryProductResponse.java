package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class GroceryProductResponse {
    private Long id;
    private Long storeId;
    private String storeName;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String imageUrl;
    private String brand;
    private String unit;
    private BigDecimal unitValue;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Integer stock;
    private Integer lowStockThreshold;
    private String barcode;
    private Boolean isOrganic;
    private Boolean isFeatured;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
