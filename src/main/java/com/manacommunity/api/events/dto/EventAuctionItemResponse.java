package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EventAuctionItemResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String name;
    private String description;
    private String category;
    private BigDecimal basePrice;
    private BigDecimal currentBid;
    private BigDecimal minIncrement;
    private String imageEmoji;
    private String imageUrl;
    private String status;
    private Integer sortOrder;
    private Integer bidCount;
    private String leaderName;
    private String closedAt;
    private String createdAt;
}
