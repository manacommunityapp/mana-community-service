package com.manacommunity.api.commerce.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommercePaymentVerifyRequest {
    @NotBlank
    private String razorpayOrderId;
    @NotBlank
    private String razorpayPaymentId;
    @NotBlank
    private String razorpaySignature;
}
