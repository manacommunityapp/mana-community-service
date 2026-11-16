package com.manacommunity.api.fresh.fulfilment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyHandoverRequest {

    @NotBlank(message = "OTP is required")
    private String otp;

    private String qrCodeToken;
}
