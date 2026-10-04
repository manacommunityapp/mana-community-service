package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class GroceryOrderRequest {
    @NotEmpty
    private List<GroceryOrderItemRequest> items;
    private String deliveryAddress;
}
