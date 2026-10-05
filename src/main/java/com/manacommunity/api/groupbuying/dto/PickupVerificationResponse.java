package com.manacommunity.api.groupbuying.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupVerificationResponse {
    private boolean success;
    private String message;
    private GroupOrderResponse order;
}
