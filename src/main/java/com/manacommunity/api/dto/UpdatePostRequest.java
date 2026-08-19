package com.manacommunity.api.dto;

import java.time.LocalDateTime;

/**
 * Partial-update DTO for PATCH /api/posts/{id}.
 * All fields are optional — only non-null values are applied.
 */
public record UpdatePostRequest(
    String content,
    String title,
    String imageUrl,
    LocalDateTime eventDate,
    LocalDateTime eventEndDate,
    String eventVenue,
    String location,
    Double price
) {}
