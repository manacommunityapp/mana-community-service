package com.manacommunity.api.email.brevo;

import lombok.Builder;

@Builder
public record BrevoStatsDto(
        int periodDays,
        long requests,
        long delivered,
        long hardBounces,
        long softBounces,
        long clicks,
        long uniqueClicks,
        long opens,
        long uniqueOpens,
        long spamReports,
        long blocked,
        long unsubscribed,
        double deliveryRate,
        double openRate,
        double clickRate,
        double bounceRate,
        boolean isConfigured,
        boolean isLive
) {}