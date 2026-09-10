package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketOffer;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOfferResponse {

    private Long id;
    private Long listingId;
    private String listingTitle;
    private BigDecimal originalPrice;
    private BigDecimal offeredPrice;
    private BigDecimal counterPrice;
    private MarketOffer.OfferStatus status;
    private String message;
    private String counterMessage;
    private String buyerName;
    private Long buyerId;
    private String sellerName;
    private Long sellerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
