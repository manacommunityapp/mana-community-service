package com.manacommunity.api.fresh.dispatch.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ProcessSettlementRequest {
    private String payoutUtrReference;
}