package com.manacommunity.api.dto;

public record FeedSummaryCountsResponse(
        long directoryCount,
        long sportsEventsCount,
        long upcomingEventsCount,
        long myPassCount,
        long trendingCount,
        long myGroupsCount,
        long topContributorsCount,
        int myEngagementPoints,
        int myEngagementLevel,
        long officialAnnouncementsCount
) {}