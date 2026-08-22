package com.manacommunity.api.dto;

public record CommentLikeToggleResponse(
    Long commentId,
    int likesCount,
    boolean liked
) {}
