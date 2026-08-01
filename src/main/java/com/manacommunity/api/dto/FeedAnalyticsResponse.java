package com.manacommunity.api.dto;

import java.util.List;
import java.util.Map;

public record FeedAnalyticsResponse(
    long totalPosts,
    long totalComments,
    long totalReactions,
    long totalGroups,
    long activeMembers,
    double communityHealthScore,
    Map<String, Long> postsByType,
    Map<String, Long> reactionsByType,
    List<TrendingResponse> trendingTopics,
    List<EngagementScoreResponse> topContributors
) {}
