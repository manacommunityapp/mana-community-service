package com.manacommunity.api.vendor.commerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryReservationRequest {
    @NotNull
    private Long variantId;
    @NotNull
    @Min(1)
    private Integer quantity;
    private String orderReference;
}
