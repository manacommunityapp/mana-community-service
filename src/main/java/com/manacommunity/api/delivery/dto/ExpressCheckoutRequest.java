package com.manacommunity.api.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressCheckoutRequest {
    @NotBlank(message = "entryCode or orderId is required")
    private String identifier;
    private Long communityId;
    private String gateId;
}
