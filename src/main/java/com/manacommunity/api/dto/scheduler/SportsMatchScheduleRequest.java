package com.manacommunity.api.dto.scheduler;

public record SportsMatchScheduleRequest(Long homeTeamId, Long awayTeamId, String matchType, String stage, String startTime) {}
