package com.manacommunity.api.dto.scheduler;

/**
 * Result of a unified schedule save: the persisted config and how many matches
 * were committed.
 */
public record SportsScheduleSaveResponse(SportsTournamentConfigResponse config, int savedMatches) {}
