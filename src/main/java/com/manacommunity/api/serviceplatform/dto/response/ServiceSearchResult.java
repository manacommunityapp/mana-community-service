package com.manacommunity.api.serviceplatform.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ServiceSearchResult {
    private Long offeringId;
    private String offeringTitle;
    private String offeringDescription;
    private BigDecimal basePrice;
    private String pricingUnit;
    private Integer estimatedDurationMinutes;
    private Long providerId;
    private String providerName;
    private String providerType;
    private BigDecimal providerRating;
    private Integer providerTotalJobs;
    private String verificationStatus;
    private Long categoryId;
    private String categoryName;
    private Long domainId;
    private String domainName;
}
