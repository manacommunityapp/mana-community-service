package com.manacommunity.api.freshkitchen.dto;

import com.manacommunity.api.freshkitchen.model.ExecutionMode;
import com.manacommunity.api.freshkitchen.model.FoodCategory;
import com.manacommunity.api.freshkitchen.model.FreshFoodProductConfig;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshProductDto {
    private Long id;
    private String productId;
    private String name;
    private String description;
    private FoodCategory category;
    private ExecutionMode executionMode;
    private String unit;
    private String packSize;
    private BigDecimal unitPrice;
    private Integer shelfLifeHours;
    private BigDecimal storageTempMin;
    private BigDecimal storageTempMax;
    private Integer minOrderLeadHours;
    private String imageUrl;
    private Boolean isActive;

    public static FreshProductDto fromEntity(FreshFoodProductConfig entity) {
        if (entity == null) {
            return null;
        }
        return FreshProductDto.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .executionMode(entity.getExecutionMode())
                .unit(entity.getUnit())
                .packSize(entity.getPackSize())
                .unitPrice(entity.getUnitPrice())
                .shelfLifeHours(entity.getShelfLifeHours())
                .storageTempMin(entity.getStorageTempMin())
                .storageTempMax(entity.getStorageTempMax())
                .minOrderLeadHours(entity.getMinOrderLeadHours())
                .imageUrl(entity.getImageUrl())
                .isActive(entity.getIsActive())
                .build();
    }
}
