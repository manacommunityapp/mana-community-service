package com.manacommunity.api.sports.engine;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.sports.dto.SportsGenericScoreRequest;
import com.manacommunity.api.sports.scheduler.SportsMatchPeriodScore;
import com.manacommunity.api.sports.scheduler.SportsScoringConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BadmintonScoringEngine implements ScoringEngine {

    @Override
    public String getSportType() {
        return "BADMINTON";
    }

    @Override
    public void validateScoreEvent(SportsTournamentMatch match, SportsGenericScoreRequest req) {
        if (req.pointsAwarded() != null && req.pointsAwarded() <= 0) {
            throw new InvalidInputException("Badminton points awarded must be positive.");
        }
    }

    @Override
    public boolean checkPeriodWon(SportsTournamentMatch match, SportsMatchPeriodScore period, SportsScoringConfig config) {
        int a = period.getScoreTeamA();
        int b = period.getScoreTeamB();
        int target = config != null && config.getPointsToWinPeriod() != null ? config.getPointsToWinPeriod() : 21;
        int maxCap = config != null && config.getTiebreakPointsToWin() != null ? config.getTiebreakPointsToWin() : 30;

        // BWF Rule: At 29-29, side scoring 30th point wins
        if (a >= maxCap || b >= maxCap) {
            return true;
        }

        // Must reach at least target and lead by 2 points
        return (a >= target || b >= target) && Math.abs(a - b) >= 2;
    }

    @Override
    public boolean checkMatchWon(SportsTournamentMatch match, List<SportsMatchPeriodScore> periods, SportsScoringConfig config) {
        int periodsToWin = config != null && config.getPeriodsToWin() != null ? config.getPeriodsToWin() : 2;
        int wonA = 0;
        int wonB = 0;

        for (SportsMatchPeriodScore ps : periods) {
            if (checkPeriodWon(match, ps, config)) {
                if (ps.getScoreTeamA() > ps.getScoreTeamB()) wonA++;
                else if (ps.getScoreTeamB() > ps.getScoreTeamA()) wonB++;
            }
        }

        return wonA >= periodsToWin || wonB >= periodsToWin;
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

    /**
     * Helper to determine service court:
     * Right court on even score, Left court on odd score.
     */
    public String getServiceCourt(int serverScore) {
        return (serverScore % 2 == 0) ? "RIGHT" : "LEFT";
    }
}
