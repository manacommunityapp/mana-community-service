package com.manacommunity.api.kyc.dto;

import lombok.Data;

@Data
public class AadhaarVerifyRequest {
    private Long requestId;
    private String otp;
}
