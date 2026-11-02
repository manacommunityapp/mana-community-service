package com.manacommunity.api.freshkitchen.dto;

import com.manacommunity.api.freshkitchen.model.ExecutionMode;
import com.manacommunity.api.freshkitchen.model.FoodCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasketItemRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    private String productName;

    @Min(value = 1, message = "quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    private BigDecimal unitPrice;

    private FoodCategory category;

    private ExecutionMode executionMode;

    @Builder.Default
    private Boolean mon = true;

    @Builder.Default
    private Boolean tue = true;

    @Builder.Default
    private Boolean wed = true;

    @Builder.Default
    private Boolean thu = true;

    @Builder.Default
    private Boolean fri = true;

    @Builder.Default
    private Boolean sat = true;

    @Builder.Default
    private Boolean sun = true;

    private String daysOfWeek;

    private String specialInstructions;
}
