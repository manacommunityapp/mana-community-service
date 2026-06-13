package com.manacommunity.api.dto;

import java.time.LocalDateTime;

public record PostResponse(
    Long id,
    String content,
    String imageUrl,
    boolean official,
    int likesCount,
    int commentsCount,
    boolean likedByCurrentUser,
    Long authorId,
    String authorName,
    String authorAvatar,
    String authorRole,
    LocalDateTime createdAt
) {}
