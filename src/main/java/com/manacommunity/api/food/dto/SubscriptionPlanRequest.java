package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubscriptionPlanRequest {
    @NotBlank
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
}
