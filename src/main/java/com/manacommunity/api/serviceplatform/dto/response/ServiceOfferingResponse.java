package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ServiceOfferingResponse {
    private Long id;
    private Long providerId;
    private String providerName;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private BigDecimal basePrice;
    private String pricingUnit;
    private Integer estimatedDurationMinutes;
    private BigDecimal minOrderValue;
    private boolean available;
    private String customFieldValues;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
