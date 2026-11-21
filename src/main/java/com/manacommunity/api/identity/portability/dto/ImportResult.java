package com.manacommunity.api.identity.portability.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ImportResult {
    private Long residentId;
    private boolean kycAutoVerified;
    private Double reputationScoreCopied;
    private Map<String, Object> importedPayload;
    private String message;
}
