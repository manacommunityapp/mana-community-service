package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketListing;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketListingResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String priceUnit;
    private String category;
    private MarketListing.Condition condition;
    private String warranty;
    private MarketListing.ListingStatus status;
    private MarketListing.TransactionMode transactionMode;
    private MarketListing.ListingVisibility visibility;
    private String location;
    private List<String> imageUrls;
    private SellerSummary seller;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerSummary {
        private Long id;
        private String fullName;
        private boolean verified;
        private String apartmentNumber;
        private String tower;
    }
}
