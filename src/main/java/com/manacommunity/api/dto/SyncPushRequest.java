package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SyncPushRequest(
    @NotEmpty List<SyncAction> actions
) {
    public record SyncAction(
        @NotNull String id,
        @NotNull String service,
        @NotNull String method,
        Map<String, Object> args,
        @NotNull LocalDateTime timestamp
    ) {}
}
