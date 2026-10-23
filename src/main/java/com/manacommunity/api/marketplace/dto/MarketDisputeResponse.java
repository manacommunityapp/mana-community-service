package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketDispute;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDisputeResponse {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long raisedById;
    private String raisedByName;
    private Long communityId;
    private String reason;
    private String description;
    private MarketDispute.DisputeStatus status;
    private String resolutionNotes;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
