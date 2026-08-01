package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GroceryOrderRequest {
    @NotNull
    private Long storeId;
    private String deliveryAddress;
    private String deliverySlot;
    private String paymentMethod;
    private List<GroceryOrderItemRequest> items;
}
