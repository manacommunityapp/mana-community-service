package com.manacommunity.api.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkflowTransitionRequest(
        @NotBlank String toStatus,
        String reason,
        String idempotencyKey
) {}
