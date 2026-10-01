package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import java.time.LocalDateTime;

/**
 * Response DTO for schedule generation log entries.
 */
public record SportsScheduleGenerationLogResponse(
    Long id,
    Long configId,
    Long eventId,
    Long communityId,
    Long generatedBy,
    String generatedByName,
    String action,
    String status,
    String tournamentType,
    Integer totalTeams,
    Integer totalMatches,
    Integer totalGroups,
    Long durationMs,
    String errorMessage,
    LocalDateTime createdAt
) {}
