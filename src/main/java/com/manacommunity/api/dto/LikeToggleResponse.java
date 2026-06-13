package com.manacommunity.api.dto;

public record LikeToggleResponse(
    int likesCount,
    boolean liked
) {}
