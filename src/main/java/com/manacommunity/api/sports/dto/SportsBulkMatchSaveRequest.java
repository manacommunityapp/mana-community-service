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
        String  stage,       // "GROUP" | "PLAYOFF" | "KNOCKOUT"
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
        String  status       // "SCHEDULED"
    ) {}
}
