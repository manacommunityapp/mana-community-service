package com.manacommunity.api.fresh.dispatch.dto;

import com.manacommunity.api.fresh.dispatch.model.HandoverStatus;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class DeliveryItemDto {
    private Long id;
    private Long commerceOrderId;
    private String customerName;
    private String tower;
    private String flatNo;
    private Integer sortSequence;
    private HandoverStatus handoverStatus;
    private Instant deliveredAt;
    private String proofImageUrl;
    private String notes;
}