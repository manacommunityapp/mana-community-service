package com.manacommunity.api.kyc.dto;

import lombok.Data;

@Data
public class KycSubmitRequest {
    private String documentType;
    private String documentNumber;
    private String frontImageBase64;
    private String backImageBase64;
}
