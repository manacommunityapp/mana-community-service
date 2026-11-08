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
public class TennisScoringEngine implements ScoringEngine {

    @Override
    public String getSportType() {
        return "TENNIS";
    }

    @Override
    public void validateScoreEvent(SportsTournamentMatch match, SportsGenericScoreRequest req) {
        if (req.pointsAwarded() != null && req.pointsAwarded() <= 0) {
            throw new InvalidInputException("Tennis points awarded must be positive.");
        }
    }

    @Override
    public boolean checkPeriodWon(SportsTournamentMatch match, SportsMatchPeriodScore period, SportsScoringConfig config) {
        int a = period.getScoreTeamA();
        int b = period.getScoreTeamB();
        int target = config != null && config.getPointsToWinPeriod() != null ? config.getPointsToWinPeriod() : 6;
        int tiebreakTarget = config != null && config.getTiebreakPointsToWin() != null ? config.getTiebreakPointsToWin() : 7;

        // Standard Tennis Set:
        // Win with 6 games and 2-game margin (e.g. 6-4, 6-3, 6-2, 6-1, 6-0).
        // If 5-5 -> 7-5.
        // If 6-6 -> 7-6 tiebreak.
        if ((a >= target || b >= target) && Math.abs(a - b) >= 2) {
            return true;
        }
        if (a == 7 && b == 6 || b == 7 && a == 6) {
            return true;
        }
        return false;
    }

    @Override
    public boolean checkMatchWon(SportsTournamentMatch match, List<SportsMatchPeriodScore> periods, SportsScoringConfig config) {
        int setsToWin = config != null && config.getPeriodsToWin() != null ? config.getPeriodsToWin() : 2;
        int wonA = 0;
        int wonB = 0;

        for (SportsMatchPeriodScore ps : periods) {
            if (checkPeriodWon(match, ps, config)) {
                if (ps.getScoreTeamA() > ps.getScoreTeamB()) wonA++;
                else if (ps.getScoreTeamB() > ps.getScoreTeamA()) wonB++;
            }
        }

        return wonA >= setsToWin || wonB >= setsToWin;
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
     * Converts raw game points to tennis notation: 0 -> Love, 1 -> 15, 2 -> 30, 3 -> 40, Deuce, Ad
     */
    public String formatGameScore(int pointsA, int pointsB) {
        if (pointsA >= 3 && pointsB >= 3) {
            if (pointsA == pointsB) return "Deuce";
            if (pointsA == pointsB + 1) return "Advantage Team A";
            if (pointsB == pointsA + 1) return "Advantage Team B";
            return pointsA > pointsB ? "Game Team A" : "Game Team B";
        }
        String[] terms = {"0", "15", "30", "40"};
        return terms[Math.min(pointsA, 3)] + " - " + terms[Math.min(pointsB, 3)];
    }
}
