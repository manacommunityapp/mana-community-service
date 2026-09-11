package com.manacommunity.api.dto.scheduler;

import jakarta.validation.constraints.NotBlank;

public record SportsRescheduleRequest(
    @NotBlank String scheduledAt,
    String venue
) {}
