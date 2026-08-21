package com.manacommunity.api.dto;

import java.time.LocalDateTime;

public record CommentLikerResponse(
    Long userId,
    String fullName,
    String profilePicUrl,
    String role,
    LocalDateTime createdAt
) {}
