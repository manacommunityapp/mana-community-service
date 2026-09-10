package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketGroupOrder;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MarketGroupOrderDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Title is required")
        private String title;
        private String description;
        @NotBlank(message = "Category is required")
        private String category;
        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.01")
        private BigDecimal basePrice;
        @NotNull(message = "Target quantity is required")
        private Integer targetQuantity;
        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        private LocalDateTime expiresAt;
        private List<TierDto> discountTiers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierDto {
        private Integer minQuantity;
        private BigDecimal discountedPrice;
        private BigDecimal discountPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private String category;
        private BigDecimal basePrice;
        private Integer targetQuantity;
        private Integer currentQuantity;
        private BigDecimal currentPrice;
        private LocalDateTime expiresAt;
        private MarketGroupOrder.GroupOrderStatus status;
        private Long organizerId;
        private String organizerName;
        private Long communityId;
        private List<TierDto> discountTiers;
        private int participantsCount;
        private LocalDateTime createdAt;
    }
}
