package com.manacommunity.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
    Long id,
    Long postId,
    String content,
    Long authorId,
    String authorName,
    String authorAvatar,
    String authorRole,
    String authorProfilePic,
    LocalDateTime createdAt,
    Long parentId,
    int likesCount,
    int repliesCount,
    boolean pinned,
    boolean acceptedAnswer,
    List<CommentResponse> replies
) {}
