package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFulfillmentUpdateRequest {
    @NotNull(message = "Status is required")
    private String status; // PREPARING, OUT_FOR_DELIVERY, READY_FOR_PICKUP, DELIVERED, PICKED_UP, CANCELLED

    private String trackingNumber;
    private String deliveryPartnerName;
    private String deliveryPartnerPhone;
    private String notes;
}
