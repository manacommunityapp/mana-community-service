package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketRequestOffer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketRequestOfferDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotNull(message = "Offered price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        private BigDecimal offeredPrice;

        private Long listingId;

        @Size(max = 500, message = "Message must not exceed 500 characters")
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long requestId;
        private Long sellerId;
        private String sellerName;
        private Long listingId;
        private BigDecimal offeredPrice;
        private String message;
        private MarketRequestOffer.OfferStatus status;
        private LocalDateTime createdAt;
    }
}
