package com.manacommunity.api.analytics.dto;

import com.manacommunity.api.analytics.entity.CommunitySaasSubscription;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record CommunitySubscriptionResponse(
        Long id,
        Long communityId,
        String externalReference,
        String planCode,
        String planName,
        BigDecimal recurringAmount,
        int billingPeriodMonths,
        CommunitySaasSubscription.Status status,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd) {
}
