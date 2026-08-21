package com.manacommunity.api.dto;

import java.time.LocalDateTime;

public record PostLikerResponse(
    Long userId,
    String fullName,
    String profilePicUrl,
    String role,
    String reactionType,
    LocalDateTime createdAt
) {}
