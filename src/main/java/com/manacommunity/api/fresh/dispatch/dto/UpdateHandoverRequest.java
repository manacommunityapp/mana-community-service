package com.manacommunity.api.fresh.dispatch.dto;

import com.manacommunity.api.fresh.dispatch.model.HandoverStatus;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class UpdateHandoverRequest {
    private HandoverStatus handoverStatus;
    private String proofImageUrl;
    private String notes;
}