package com.manacommunity.api.dto.dashboard;

import java.util.List;

/**
 * Data Transfer Object representing comprehensive sports analytics metrics.
 * Restricted to Admin and Sports Admin roles.
 */
public record SportsAnalyticsResponse(
        OverviewMetrics overview,
        List<SportParticipationMetric> sportParticipation,
        List<RegistrationStatusMetric> registrationStatuses,
        GenderBreakdown genderBreakdown,
        AgeGroupBreakdown ageGroupBreakdown,
        List<TournamentMetric> topTournaments,
        List<VenueUtilizationMetric> venueUtilization,
        AuctionAnalytics auctionSummary
) {

    public record OverviewMetrics(
            long totalTournaments,
            long activeTournaments,
            long totalEvents,
            long activeEvents,
            long totalRegistrations,
            long confirmedRegistrations,
            long pendingRegistrations,
            long uniqueParticipants,
            long totalVenues
    ) {}

    public record SportParticipationMetric(
            Long sportId,
            String sportName,
            String icon,
            long eventCount,
            long totalRegistrations,
            long confirmedRegistrations
    ) {}

    public record RegistrationStatusMetric(
            String status,
            long count,
            double percentage
    ) {}

    public record GenderBreakdown(
            long maleCount,
            long femaleCount,
            long otherCount,
            double malePercentage,
            double femalePercentage
    ) {}

    public record AgeGroupBreakdown(
            long kidsUnder12,
            long youth13To19,
            long adults20To50,
            long seniorsAbove50
    ) {}

    public record TournamentMetric(
            Long tournamentId,
            String tournamentName,
            String status,
            int maxParticipants,
            long totalRegistrations,
            long confirmedRegistrations,
            double occupancyPercentage
    ) {}

    public record VenueUtilizationMetric(
            Long venueId,
            String venueName,
            long eventCount,
            long courtCount
    ) {}

    public record AuctionAnalytics(
            long totalAuctionConfigs,
            long totalAuctionTeams,
            long totalAuctionPlayers,
            long totalBudgetAllocated,
            long totalBudgetSpent
    ) {}
}
