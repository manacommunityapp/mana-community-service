package com.manacommunity.api.kyc.dto;

import lombok.Data;

@Data
public class DocumentSubmitRequest {
    private Long communityId;
    private String idType;
    private String idNumberHash;  // SHA-256 of ID number, computed client-side
    private String documentUrl;
}
