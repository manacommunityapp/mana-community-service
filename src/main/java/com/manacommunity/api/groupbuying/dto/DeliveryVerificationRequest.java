package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryVerificationRequest {
    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotBlank(message = "Delivery OTP is required")
    private String deliveryOtp;
}
