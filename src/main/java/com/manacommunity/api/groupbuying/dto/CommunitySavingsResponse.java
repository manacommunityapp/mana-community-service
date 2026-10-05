package com.manacommunity.api.groupbuying.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunitySavingsResponse {
    private BigDecimal totalSavedThisMonth;
    private Integer totalOrders;
    private Integer activeDeals;
    private BigDecimal avgSavingPerOrder;
    private Integer totalKgsBought;
    private String topCategory;
    private BigDecimal totalSavedAllTime;
    
    // Community Buying Power & Collective Achievement
    private Double collectiveDiscountPercent;
    private String heroMilestoneText;
    private List<TopDealSavingsDto> topDealsThisMonth;
    private List<TowerSavingsDto> towerLeaderboard;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopDealSavingsDto {
        private String dealTitle;
        private BigDecimal savings;
        private Integer participants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TowerSavingsDto {
        private String tower;
        private Integer orders;
        private BigDecimal totalSaved;
    }
}
