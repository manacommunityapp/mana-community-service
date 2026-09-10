package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketListing;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketWishlistResponse {

    private Long id;
    private Long listingId;
    private String listingTitle;
    private BigDecimal listingPrice;
    private String listingCategory;
    private MarketListing.ListingStatus listingStatus;
    private String listingImageUrl;
    private String sellerName;
    private Long sellerId;
    private LocalDateTime addedAt;
}
