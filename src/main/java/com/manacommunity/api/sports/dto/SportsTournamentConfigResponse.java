package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SportsTournamentConfigResponse(
    Long    id,
    String  tournamentName,
    String  tournamentType,
    Long    sportId,
    String  sportName,
    Long    communityId,
    String  communityName,
    Long    eventId,
    String  eventName,
    Long    tournamentId,
    Integer totalTeams,
    Integer numberOfGroups,
    Integer teamsPerGroup,
    Integer teamsAdvancingPerGroup,
    Boolean thirdPlaceMatch,
    Boolean hasSeeding,
    Integer swissRounds,
    LocalDate startDate,
    LocalDate endDate,
    Integer matchDurationMinutes,
    Integer breakBetweenMatchesMinutes,
    Long    venueId,
    String  venueName,
    Integer pointsForWin,
    Integer pointsForDraw,
    Integer pointsForLoss,
    String  status,
    int     totalMatches,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    /** Effective showInFeed for the whole schedule (null = opt-out default = shown). */
    Boolean showInFeed,
    String  seedingOrder,
    Boolean byesFirstRoundOnly,
    Boolean showByesInBracket,
    Boolean limitMatchesPerDay,
    Integer maxMatchesPerDay,
    Boolean usePlayoffDays,
    java.util.List<Integer> playoffDays,
    String  venueAssignType,
    java.util.Map<String, String> playoffRoundDates,
    java.util.Map<String, String> playoffRoundTimes,
    java.util.Map<String, String> playoffRoundDurations,
    java.util.Map<String, String> playoffRoundModes
) {}

