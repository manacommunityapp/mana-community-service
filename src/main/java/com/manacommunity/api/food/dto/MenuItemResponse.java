package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MenuItemResponse {
    private Long id;
    private String name;
    private String slug;
    private Long categoryId;
    private String categoryName;
    private Long restaurantId;
    private String description;
    private String imageUrl;
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
    private Boolean isBestseller;
    private Integer sortOrder;
    private String tags;
    private List<VariantResponse> variants;
    private List<AddonResponse> addons;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class VariantResponse {
        private Long id;
        private String variantName;
        private BigDecimal price;
        private Boolean isDefault;
    }

    @Data
    @Builder
    public static class AddonResponse {
        private Long id;
        private String addonGroupName;
        private String addonName;
        private BigDecimal price;
        private Boolean isDefault;
        private Integer maxQuantity;
    }
}
