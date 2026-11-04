package com.manacommunity.api.fresh.processing.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class QualityCheckDto {
    private Long id;
    private Long batchId;
    private String checkName;
    private String measuredValue;
    private Boolean isPassed;
    private Long checkedByUserId;
    private String checkedByUserName;
    private Instant checkedAt;
    private String notes;
}