package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuctionBidResponse {
    private Long id;
    private Long configId;
    private Long playerId;
    private Long teamId;
    private String teamName;
    private Long bidAmount;
    private Integer incrementUsed;
    private Boolean isRtm;
    private Long bidByUserId;
    private LocalDateTime bidAt;
}
