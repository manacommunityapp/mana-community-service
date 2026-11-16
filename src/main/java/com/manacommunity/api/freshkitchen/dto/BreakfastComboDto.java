package com.manacommunity.api.freshkitchen.dto;

import com.manacommunity.api.freshkitchen.model.BreakfastCombo;
import com.manacommunity.api.freshkitchen.model.BreakfastComboItem;
import com.manacommunity.api.freshkitchen.model.ComboItemRole;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakfastComboDto {

    private Long id;
    private String comboCode;
    private String name;
    private String description;
    private String category;
    private BigDecimal basePrice;
    private BigDecimal discountedPrice;
    private String imageUrl;
    private Boolean isActive;

    @Builder.Default
    private List<BreakfastComboItemDto> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakfastComboItemDto {
        private Long id;
        private String productId;
        private String productName;
        private ComboItemRole itemRole;
        private Integer defaultQuantity;
        private BigDecimal unitPrice;
        private Boolean isMandatory;
        private Boolean isCustomizable;
        private String allowedSubstitutes;
        private Integer sortOrder;

        public static BreakfastComboItemDto fromEntity(BreakfastComboItem entity) {
            if (entity == null) {
                return null;
            }
            return BreakfastComboItemDto.builder()
                    .id(entity.getId())
                    .productId(entity.getProductId())
                    .productName(entity.getProductName())
                    .itemRole(entity.getItemRole())
                    .defaultQuantity(entity.getDefaultQuantity())
                    .unitPrice(entity.getUnitPrice())
                    .isMandatory(entity.getIsMandatory())
                    .isCustomizable(entity.getIsCustomizable())
                    .allowedSubstitutes(entity.getAllowedSubstitutes())
                    .sortOrder(entity.getSortOrder())
                    .build();
        }
    }

    public static BreakfastComboDto fromEntity(BreakfastCombo entity) {
        if (entity == null) {
            return null;
        }
        List<BreakfastComboItemDto> itemDtos = new ArrayList<>();
        if (entity.getItems() != null) {
            for (BreakfastComboItem item : entity.getItems()) {
                itemDtos.add(BreakfastComboItemDto.fromEntity(item));
            }
        }
        return BreakfastComboDto.builder()
                .id(entity.getId())
                .comboCode(entity.getComboCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .basePrice(entity.getBasePrice())
                .discountedPrice(entity.getDiscountedPrice())
                .imageUrl(entity.getImageUrl())
                .isActive(entity.getIsActive())
                .items(itemDtos)
                .build();
    }
}
