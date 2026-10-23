package com.manacommunity.api.groupbuying.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupVerificationRequest {
    private String qrToken;
    private String orderNumber;
    private String collectorPin;
}
