package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MenuItemRequest {
    @NotBlank
    private String name;
    @NotNull
    private Long categoryId;
    private String description;
    private String imageUrl;
    @NotNull
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Boolean isVeg;
    private Boolean isVegan;
    private Boolean isJain;
    private Integer spiceLevel;
    private Integer calories;
    private BigDecimal protein;
    private BigDecimal carbs;
    private BigDecimal fat;
    private BigDecimal fiber;
    private Integer preparationTime;
    private Boolean isAvailable;
    private Boolean isFeatured;
    private String tags;
    private List<VariantRequest> variants;
    private List<AddonRequest> addons;
}
