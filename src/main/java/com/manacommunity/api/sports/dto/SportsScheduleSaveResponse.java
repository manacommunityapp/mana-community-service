package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

/**
 * Result of a unified schedule save: the persisted config and how many matches
 * were committed.
 */
public record SportsScheduleSaveResponse(SportsTournamentConfigResponse config, int savedMatches) {}
