package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class CateringRequestResponse {
    private Long id;
    private Long userId;
    private String occasionType;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String venue;
    private Integer guestCount;
    private BigDecimal budget;
    private String menuPreferences;
    private String dietaryRequirements;
    private String status;
    private List<QuotationSummary> quotations;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class QuotationSummary {
        private Long id;
        private Long catererId;
        private String catererName;
        private BigDecimal pricePerPlate;
        private BigDecimal totalAmount;
        private String status;
    }
}
