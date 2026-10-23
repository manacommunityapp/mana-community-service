package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketGroupOrderParticipant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketGroupOrderParticipantDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRequest {
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long groupOrderId;
        private Long userId;
        private String userName;
        private Integer quantity;
        private BigDecimal lockedPrice;
        private BigDecimal totalPaid;
        private MarketGroupOrderParticipant.PaymentStatus paymentStatus;
        private LocalDateTime joinedAt;
    }
}
