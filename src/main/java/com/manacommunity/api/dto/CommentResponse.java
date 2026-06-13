package com.manacommunity.api.dto;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    Long postId,
    String content,
    Long authorId,
    String authorName,
    String authorAvatar,
    String authorRole,
    LocalDateTime createdAt
) {}
