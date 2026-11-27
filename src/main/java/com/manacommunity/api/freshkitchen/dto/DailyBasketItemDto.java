package com.manacommunity.api.freshkitchen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.manacommunity.api.freshkitchen.model.DailyBasketItem;
import com.manacommunity.api.freshkitchen.model.ExecutionMode;
import com.manacommunity.api.freshkitchen.model.FoodCategory;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    private List<String> scheduledDays;

    @JsonProperty("itemName")
    public String getItemName() {
        return productName;
    }

    @JsonProperty("isPaused")
    public Boolean isPaused() {
        return !Boolean.TRUE.equals(isActive);
    }

    public static DailyBasketItemDto fromEntity(DailyBasketItem entity) {
        if (entity == null) {
            return null;
        }
        BigDecimal unitPrice = entity.getUnitPrice() != null ? entity.getUnitPrice() : BigDecimal.ZERO;
        int qty = entity.getQuantity() != null ? entity.getQuantity() : 0;
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));

        List<String> days = new ArrayList<>();
        if (Boolean.TRUE.equals(entity.getMon())) days.add("MON");
        if (Boolean.TRUE.equals(entity.getTue())) days.add("TUE");
        if (Boolean.TRUE.equals(entity.getWed())) days.add("WED");
        if (Boolean.TRUE.equals(entity.getThu())) days.add("THU");
        if (Boolean.TRUE.equals(entity.getFri())) days.add("FRI");
        if (Boolean.TRUE.equals(entity.getSat())) days.add("SAT");
        if (Boolean.TRUE.equals(entity.getSun())) days.add("SUN");

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
                .scheduledDays(days)
                .build();
    }
}
