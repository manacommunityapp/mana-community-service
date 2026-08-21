package com.manacommunity.api.email.brevo;

import lombok.Builder;

@Builder
public record BrevoSyncResultDto(
        int syncedCount,
        int openedUpdated,
        int bouncedUpdated,
        String message,
        String timestamp
) {}