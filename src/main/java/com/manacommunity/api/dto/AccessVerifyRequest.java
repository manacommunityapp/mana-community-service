package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;

public record AccessVerifyRequest(
    @NotNull Long turnstileId,
    Long userId,
    @NotNull String direction,
    @NotNull String method,
    Boolean granted,
    String denialReason
) {}
