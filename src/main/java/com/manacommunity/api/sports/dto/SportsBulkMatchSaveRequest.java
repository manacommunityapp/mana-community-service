package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import java.util.List;

public record SportsBulkMatchSaveRequest(List<MatchData> matches) {

    public record MatchData(
        Long    eventId,
        Long    configId,
        String  groupName,   // "Group A", "Playoffs", match name for knockout
        String  stage,       // "GROUP" | "PLAYOFF" | "KNOCKOUT" | "ROUND_ROBIN"
        String  round,       // "ROUND_OF_64" | "ROUND_OF_32" | "ROUND_OF_16" | "QUARTER_FINAL" | "SEMI_FINAL" | "THIRD_PLACE" | "FINAL"
        Integer roundNumber,
        Integer bracketSlot,
        Integer matchNumber,
        String  homeName,
        String  homeId,      // string team ID from UI scheduler (may be numeric or "TBD")
        String  awayName,
        String  awayId,
        String  matchDate,   // "yyyy-MM-dd"
        String  matchTime,   // "HH:mm"
        Integer duration,    // minutes
        Long    venueId,
        Long    courtId,
        String  status,      // "SCHEDULED", "DRAFT", "PUBLISHED", "AUTO_ADVANCED"
        String  homeFlat,
        String  awayFlat
    ) {
        public MatchData(
            Long    eventId,
            Long    configId,
            String  groupName,
            String  stage,
            String  round,
            Integer roundNumber,
            Integer bracketSlot,
            Integer matchNumber,
            String  homeName,
            String  homeId,
            String  awayName,
            String  awayId,
            String  matchDate,
            String  matchTime,
            Integer duration,
            Long    venueId,
            Long    courtId,
            String  status
        ) {
            this(eventId, configId, groupName, stage, round, roundNumber, bracketSlot,
                 matchNumber, homeName, homeId, awayName, awayId,
                 matchDate, matchTime, duration, venueId, courtId, status, null, null);
        }

        public MatchData(
            Long eventId, Long configId, String groupName, String stage,
            Integer matchNumber, String homeName, String homeId,
            String awayName, String awayId, String matchDate, String matchTime,
            Integer duration, Long venueId, Long courtId, String status
        ) {
            this(eventId, configId, groupName, stage, null, null, null,
                 matchNumber, homeName, homeId, awayName, awayId,
                 matchDate, matchTime, duration, venueId, courtId, status, null, null);
        }
    }
}

