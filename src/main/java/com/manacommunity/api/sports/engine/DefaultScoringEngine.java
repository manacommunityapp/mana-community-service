package com.manacommunity.api.sports.engine;

import com.manacommunity.api.sports.dto.SportsGenericScoreRequest;
import com.manacommunity.api.sports.scheduler.SportsMatchPeriodScore;
import com.manacommunity.api.sports.scheduler.SportsScoringConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DefaultScoringEngine implements ScoringEngine {

    @Override
    public String getSportType() {
        return "DEFAULT";
    }

    @Override
    public void validateScoreEvent(SportsTournamentMatch match, SportsGenericScoreRequest req) {
        // default passes all valid inputs
    }

    @Override
    public boolean checkPeriodWon(SportsTournamentMatch match, SportsMatchPeriodScore period, SportsScoringConfig config) {
        if (config == null || config.getPointsToWinPeriod() == null) return false;
        int a = period.getScoreTeamA();
        int b = period.getScoreTeamB();
        int target = config.getPointsToWinPeriod();

        if (Boolean.TRUE.equals(config.getMustWinByTwo())) {
            return (a >= target || b >= target) && Math.abs(a - b) >= 2;
        }
        return a >= target || b >= target;
    }

    @Override
    public boolean checkMatchWon(SportsTournamentMatch match, List<SportsMatchPeriodScore> periods, SportsScoringConfig config) {
        if (config == null) return false;
        if (config.getPeriodsToWin() != null) {
            int wonA = 0;
            int wonB = 0;
            for (SportsMatchPeriodScore ps : periods) {
                if (checkPeriodWon(match, ps, config)) {
                    if (ps.getScoreTeamA() > ps.getScoreTeamB()) wonA++;
                    else if (ps.getScoreTeamB() > ps.getScoreTeamA()) wonB++;
                }
            }
            return wonA >= config.getPeriodsToWin() || wonB >= config.getPeriodsToWin();
        }
        return config.getPeriodsCount() != null && periods.size() >= config.getPeriodsCount();
    }

    @Override
    public String formatScoreTeamA(List<SportsMatchPeriodScore> periods) {
        String score = periods.stream()
                .map(p -> String.valueOf(p.getScoreTeamA()))
                .collect(Collectors.joining(", "));
        return score.isEmpty() ? "0" : score;
    }

    @Override
    public String formatScoreTeamB(List<SportsMatchPeriodScore> periods) {
        String score = periods.stream()
                .map(p -> String.valueOf(p.getScoreTeamB()))
                .collect(Collectors.joining(", "));
        return score.isEmpty() ? "0" : score;
    }
}
