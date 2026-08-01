package com.cpn.web.common;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Map<String, Object> meta,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> success(String message, T data, Map<String, Object> meta) {
        return new ApiResponse<>(true, message, data, meta, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> error(String message, Map<String, Object> meta) {
        return new ApiResponse<>(false, message, null, meta, LocalDateTime.now());
    }
}
