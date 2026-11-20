package com.manacommunity.api.platform.web;

import java.time.Instant;

/**
 * Standard response envelope for all API endpoints.
 * New controllers should return {@code ApiResponse<T>} instead of raw objects.
 * Existing controllers are migrated incrementally.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String correlationId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, CorrelationContext.get(), Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, CorrelationContext.get(), Instant.now());
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, null, message, CorrelationContext.get(), Instant.now());
    }
}
