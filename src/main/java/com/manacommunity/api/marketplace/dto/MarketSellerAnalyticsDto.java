package com.manacommunity.api.marketplace.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketSellerAnalyticsDto {

    private Long sellerId;
    private int activeListings;
    private int soldListings;
    private BigDecimal totalRevenueGmv;
    private int totalOrders;
    private int totalOffersReceived;
    private int totalReviews;
    private double averageRating;
    private double responseRatePercent;
}
