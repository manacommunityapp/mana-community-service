package com.manacommunity.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroceryOrderItemResponse {
    private Long id;
    private Long groceryItemId;
    private String groceryItemName;
    private Integer quantity;
    private BigDecimal subtotal;
}
