package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderItemRequest {
    @NotNull
    private Long itemId;
    @NotNull
    private Integer quantity;
    private String variantName;
    private String specialInstructions;
    private List<AddonRequest> addons;
}
