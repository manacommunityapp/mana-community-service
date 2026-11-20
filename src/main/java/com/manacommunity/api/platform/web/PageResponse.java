package com.manacommunity.api.platform.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard pagination wrapper for list endpoints.
 * Use {@link #of(Page)} to construct from a Spring Data {@code Page<T>}.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
