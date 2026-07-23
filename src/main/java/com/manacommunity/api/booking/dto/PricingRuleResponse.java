package com.manacommunity.api.booking.dto;

import com.manacommunity.api.booking.entity.enums.PricingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PricingRuleResponse {

    private Long id;
    private Long resourceId;
    private Long categoryId;
    private PricingType pricingType;
    private BigDecimal amount;
    private BigDecimal percentage;
    private String description;
    private Boolean isActive;
    private String validFrom;
    private String validTo;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
}
