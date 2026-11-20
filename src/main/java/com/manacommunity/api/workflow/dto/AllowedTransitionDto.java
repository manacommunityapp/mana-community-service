package com.manacommunity.api.workflow.dto;

public record AllowedTransitionDto(
        String toStatus,
        String displayLabel,
        boolean reasonRequired
) {}
