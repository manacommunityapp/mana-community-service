package com.manacommunity.api.vendor.commerce.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationResponse {
    private boolean success;
    private String message;
    private Long variantId;
    private Integer reservedQty;
    private Integer remainingAvailableQty;
}
