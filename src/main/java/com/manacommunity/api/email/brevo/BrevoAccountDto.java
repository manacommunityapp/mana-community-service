package com.manacommunity.api.email.brevo;

import lombok.Builder;

@Builder
public record BrevoAccountDto(
        String email,
        String firstName,
        String lastName,
        String companyName,
        String planType,
        long dailyRelayQuota,
        long creditsRemaining,
        long creditsUsed,
        boolean isConfigured,
        boolean isLive,
        String message
) {}