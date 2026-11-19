package com.manacommunity.api.workflow.dto;

import java.time.Instant;

public record TransitionHistoryDto(
        Long id,
        String fromStatus,
        String toStatus,
        String actorRole,
        Long actorUserId,
        String reason,
        Instant transitionedAt,
        String displayLabel
) {}
