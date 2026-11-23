package com.manacommunity.api.fulfillment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class ListingDtos {

    public record CreateListingRequest(
            Long sellerId,
            String title,
            String description,
            String category,
            BigDecimal price,
            String priceUnit,
            Boolean slotBased
    ) {}

    public record ListingResponse(
            Long id,
            Long sellerId,
            String sellerName,
            Long communityId,
            String title,
            String description,
            String category,
            BigDecimal price,
            String priceUnit,
            Boolean isAvailable,
            Boolean slotBased,
            Instant createdAt
    ) {}
}
