package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SyncPullRequest(
    @NotNull LocalDateTime since
) {}
