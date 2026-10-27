package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class JourneyMatchProposalDto {
    private Long id;
    private Long requestId;
    private String status;
    private Instant createdAt;
    private MobilityCarpoolOfferDto offer;
}
