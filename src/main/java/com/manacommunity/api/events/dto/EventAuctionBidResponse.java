package com.manacommunity.api.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAuctionBidResponse {
    private Long id;
    private Long itemId;
    private String itemName;
    private Long eventId;
    private Long bidderUserId;
    private String bidderName;
    private BigDecimal amount;
    private String bidAt;
    private String timeAgo;
}
