package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroceryOrderItemRequest {
    @NotNull
    private Long productId;
    @NotNull
    private Integer quantity;
}
