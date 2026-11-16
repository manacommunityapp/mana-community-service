package com.manacommunity.api.fresh.processing.dto;

import com.manacommunity.api.fresh.processing.model.ReleaseDecision;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class BatchReleaseRequest {
    private Long releasedByUserId;
    private ReleaseDecision releaseDecision;
    private BigDecimal temperatureVerifiedCelsius;
    private String remarks;
}