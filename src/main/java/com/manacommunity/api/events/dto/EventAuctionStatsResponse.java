package com.manacommunity.api.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAuctionStatsResponse {
    private BigDecimal totalRevenue;
    private int totalItems;
    private int liveItemsCount;
    private int closedItemsCount;
    private int upcomingItemsCount;
    private int totalBidsCount;
    private List<EventAuctionLeaderboardEntry> leaderboard;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventAuctionLeaderboardEntry {
        private int rank;
        private String name;
        private BigDecimal totalAmount;
        private int bidCount;
    }
}
