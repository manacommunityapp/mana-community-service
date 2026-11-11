package com.manacommunity.api.sports.dto;

public record SportsMatchScheduleRequest(
    Long homeTeamId,
    Long awayTeamId,
    String matchType,
    String stage,
    String startTime,
    Long venueId,
    Long courtId,
    Integer durationMinutes,
    String homeName,
    String awayName,
    String homeFlat,
    String awayFlat
) {
    public SportsMatchScheduleRequest(Long homeTeamId, Long awayTeamId, String matchType, String stage, String startTime) {
        this(homeTeamId, awayTeamId, matchType, stage, startTime, null, null, null, null, null, null, null);
    }

    public SportsMatchScheduleRequest(Long homeTeamId, Long awayTeamId, String matchType, String stage, String startTime,
                                      Long venueId, Long courtId, Integer durationMinutes) {
        this(homeTeamId, awayTeamId, matchType, stage, startTime, venueId, courtId, durationMinutes, null, null, null, null);
    }
}
