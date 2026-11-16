package com.manacommunity.api.fresh.processing.dto;

import com.manacommunity.api.fresh.processing.model.ReleaseDecision;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder
public class BatchReleaseDto {
    private Long id;
    private Long batchId;
    private Long releasedByUserId;
    private String releasedByUserName;
    private ReleaseDecision releaseDecision;
    private BigDecimal temperatureVerifiedCelsius;
    private Instant releasedAt;
    private String remarks;
}