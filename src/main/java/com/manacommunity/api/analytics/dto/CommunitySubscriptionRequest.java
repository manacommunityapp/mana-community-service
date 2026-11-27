package com.manacommunity.api.analytics.dto;

import com.manacommunity.api.analytics.entity.CommunitySaasSubscription;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CommunitySubscriptionRequest {
    @NotNull private Long communityId;
    private String externalReference;
    @NotBlank private String planCode;
    @NotBlank private String planName;
    @NotNull @DecimalMin("0.00") private BigDecimal recurringAmount;
    @Min(1) private int billingPeriodMonths;
    @NotNull private CommunitySaasSubscription.Status status;
    @NotNull private LocalDate currentPeriodStart;
    private LocalDate currentPeriodEnd;
}
