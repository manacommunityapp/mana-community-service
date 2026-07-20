package com.manacommunity.api.marketplace.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WishlistResponse {

    private Long id;
    private Long listingId;
    private String listingTitle;
    private BigDecimal listingPrice;
    private String listingCategory;
    private String listingStatus;
    private String listingImageUrl;
    private String sellerName;
    private LocalDateTime addedAt;
}
