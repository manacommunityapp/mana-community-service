package com.manacommunity.api.sports.dto;

import java.util.UUID;

public record BallEventRequest(
    Long matchId,
    Integer inningsNumber,
    Long batsmanId,
    Long nonStrikerId,
    Long bowlerId,
    Integer runsScored,
    Boolean isBoundary,
    Boolean isSix,
    String extrasType,
    Integer extrasRuns,
    Boolean isWicket,
    String dismissalType,
    Long dismissedPlayerId,
    Long fielderId,
    String commentary,
    UUID commandId
) {
    public BallEventRequest(
        Long matchId,
        Integer inningsNumber,
        Long batsmanId,
        Long nonStrikerId,
        Long bowlerId,
        Integer runsScored,
        Boolean isBoundary,
        Boolean isSix,
        String extrasType,
        Integer extrasRuns,
        Boolean isWicket,
        String dismissalType,
        Long dismissedPlayerId,
        Long fielderId,
        String commentary
    ) {
        this(matchId, inningsNumber, batsmanId, nonStrikerId, bowlerId, runsScored,
             isBoundary, isSix, extrasType, extrasRuns, isWicket, dismissalType,
             dismissedPlayerId, fielderId, commentary, null);
    }
}
