package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GroceryProductRequest {
    @NotNull
    private Long storeId;
    @NotNull
    private Long categoryId;
    @NotBlank
    private String name;
    private String description;
    private String imageUrl;
    private String brand;
    private String unit;
    private BigDecimal unitValue;
    @NotNull
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Integer stock;
    private Integer lowStockThreshold;
    private String barcode;
    private Boolean isOrganic;
}
