package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VaultDocumentResponse {
    private String id;
    private String title;
    private String category;
    private String fileSize;
    private String fileFormat;
    private String lastUpdated;
    private String documentUrl;
    private boolean isConfidential;
    private String resolutionRef;
    private boolean digitalSealVerified;
}
