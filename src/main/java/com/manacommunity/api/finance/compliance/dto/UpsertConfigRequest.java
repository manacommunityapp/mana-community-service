package com.manacommunity.api.finance.compliance.dto;

import lombok.Data;

@Data
public class UpsertConfigRequest {
    private Long communityId;
    private String reportType;
    private String frequency;
    private String scheduleCron;
    private String recipients;  // JSON array string [{email, name}]
    private boolean active;
}
