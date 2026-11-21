package com.manacommunity.api.kyc.dto;

import lombok.Data;

@Data
public class AadhaarInitiateRequest {
    private String aadhaarHash;  // SHA-256 of Aadhaar number, computed client-side
    private Long communityId;
}
