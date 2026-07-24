package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionPlanResponse {
    private Long id;
    private String name;
    private String description;
    private String planType;
    private String targetAudience;
    private String providerType;
    private Long providerId;
    private BigDecimal pricePerMeal;
    private BigDecimal monthlyPrice;
    private Integer minDays;
    private Boolean includesWeekends;
    private String imageUrl;
    private Boolean active;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
