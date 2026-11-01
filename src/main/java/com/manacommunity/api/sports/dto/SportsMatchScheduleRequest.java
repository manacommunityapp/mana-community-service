package com.manacommunity.api.sports.dto;

public record SportsMatchScheduleRequest(
    Long homeTeamId,
    Long awayTeamId,
    String matchType,
    String stage,
    String startTime,
    Long venueId,
    Long courtId,
    Integer durationMinutes
) {
    public SportsMatchScheduleRequest(Long homeTeamId, Long awayTeamId, String matchType, String stage, String startTime) {
        this(homeTeamId, awayTeamId, matchType, stage, startTime, null, null, null);
    }
}
