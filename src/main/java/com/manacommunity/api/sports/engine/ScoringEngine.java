package com.manacommunity.api.sports.engine;

import com.manacommunity.api.sports.dto.SportsGenericScoreRequest;
import com.manacommunity.api.sports.scheduler.SportsMatchPeriodScore;
import com.manacommunity.api.sports.scheduler.SportsScoringConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;

import java.util.List;

public interface ScoringEngine {
    String getSportType();
    void validateScoreEvent(SportsTournamentMatch match, SportsGenericScoreRequest req);
    boolean checkPeriodWon(SportsTournamentMatch match, SportsMatchPeriodScore period, SportsScoringConfig config);
    boolean checkMatchWon(SportsTournamentMatch match, List<SportsMatchPeriodScore> periods, SportsScoringConfig config);
    String formatScoreTeamA(List<SportsMatchPeriodScore> periods);
    String formatScoreTeamB(List<SportsMatchPeriodScore> periods);
}
