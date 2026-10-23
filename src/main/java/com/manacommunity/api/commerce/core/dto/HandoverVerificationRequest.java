package com.manacommunity.api.commerce.core.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverVerificationRequest {
    private String orderNumber;
    private String enteredOtp;
    private String tokenOrOtp;
}