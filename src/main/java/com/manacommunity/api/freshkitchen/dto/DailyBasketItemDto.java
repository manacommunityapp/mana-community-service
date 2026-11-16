package com.manacommunity.api.freshkitchen.dto;

import com.manacommunity.api.freshkitchen.model.DailyBasketItem;
import com.manacommunity.api.freshkitchen.model.ExecutionMode;
import com.manacommunity.api.freshkitchen.model.FoodCategory;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyBasketItemDto {

    private Long id;
    private Long basketId;
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private FoodCategory category;
    private ExecutionMode executionMode;
    private Boolean mon;
    private Boolean tue;
    private Boolean wed;
    private Boolean thu;
    private Boolean fri;
    private Boolean sat;
    private Boolean sun;
    private String daysOfWeek;
    private Boolean isActive;
    private String specialInstructions;
    private BigDecimal totalPrice;

    public static DailyBasketItemDto fromEntity(DailyBasketItem entity) {
        if (entity == null) {
            return null;
        }
        BigDecimal unitPrice = entity.getUnitPrice() != null ? entity.getUnitPrice() : BigDecimal.ZERO;
        int qty = entity.getQuantity() != null ? entity.getQuantity() : 0;
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));

        return DailyBasketItemDto.builder()
                .id(entity.getId())
                .basketId(entity.getBasket() != null ? entity.getBasket().getId() : null)
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .category(entity.getCategory())
                .executionMode(entity.getExecutionMode())
                .mon(entity.getMon())
                .tue(entity.getTue())
                .wed(entity.getWed())
                .thu(entity.getThu())
                .fri(entity.getFri())
                .sat(entity.getSat())
                .sun(entity.getSun())
                .daysOfWeek(entity.getDaysOfWeek())
                .isActive(entity.getIsActive())
                .specialInstructions(entity.getSpecialInstructions())
                .totalPrice(total)
                .build();
    }
}
