package com.manacommunity.api.groupbuying.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupVerificationResponse {
    private boolean success;
    private boolean valid;
    private String message;
    private String orderNumber;
    private String dealTitle;
    private String userName;
    private String userFlat;
    private Integer quantity;
    private String pickupPoint;
    private GroupOrderResponse order;
}
