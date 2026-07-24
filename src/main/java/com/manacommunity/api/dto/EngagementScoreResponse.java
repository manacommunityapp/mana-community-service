package com.manacommunity.api.dto;

import java.util.List;

public record EngagementScoreResponse(
    Long userId,
    String userName,
    String profilePicUrl,
    int totalPoints,
    int level,
    int currentStreak,
    int postsCount,
    int commentsCount,
    int reactionsReceived,
    int helpfulCount,
    int volunteerPoints,
    Integer rankPosition,
    List<BadgeResponse> badges
) {
    public record BadgeResponse(
        Long id,
        String badgeType,
        String title,
        String description,
        String icon,
        int pointsValue,
        String earnedAt
    ) {}
}
