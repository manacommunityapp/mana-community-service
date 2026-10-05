package com.manacommunity.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroceryOrderItemRequest {
    @NotNull
    private Long groceryItemId;
    @NotNull
    @Min(1)
    private Integer quantity;
}
