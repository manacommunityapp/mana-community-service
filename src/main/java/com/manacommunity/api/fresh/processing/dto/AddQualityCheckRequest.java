package com.manacommunity.api.fresh.processing.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AddQualityCheckRequest {
    private String checkName;
    private String measuredValue;
    private Boolean isPassed;
    private Long checkedByUserId;
    private String notes;
}