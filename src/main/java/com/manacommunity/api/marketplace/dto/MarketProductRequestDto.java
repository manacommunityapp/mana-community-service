package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketProductRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MarketProductRequestDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        private String requestTitle;

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        private String description;

        @NotBlank(message = "Category is required")
        private String category;

        private BigDecimal targetBudget;
        private LocalDate neededByDate;
        private MarketProductRequest.UrgencyLevel urgency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String requestTitle;
        private String description;
        private String category;
        private BigDecimal targetBudget;
        private LocalDate neededByDate;
        private MarketProductRequest.UrgencyLevel urgency;
        private MarketProductRequest.RequestStatus status;
        private String requesterName;
        private Long requesterId;
        private Long communityId;
        private int offersCount;
        private List<MarketRequestOfferDto.Response> offers;
        private LocalDateTime createdAt;
    }
}
