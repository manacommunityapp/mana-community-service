package com.manacommunity.api.workflow.dto;

public record WorkflowTransitionResult(
        boolean success,
        String newStatus,
        TransitionHistoryDto historyEntry
) {}
