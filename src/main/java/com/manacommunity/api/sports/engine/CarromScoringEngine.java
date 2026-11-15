package com.manacommunity.api.sports.engine;

import com.manacommunity.api.sports.dto.SportsGenericScoreRequest;
import com.manacommunity.api.sports.scheduler.SportsMatchPeriodScore;
import com.manacommunity.api.sports.scheduler.SportsScoringConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scoring engine for Carrom.
 * In Carrom:
 * - Each "period" represents a Board (frame).
 * - Only the winner of a board scores points:
 *     (Opponent carrom men remaining, 1 pt each, max 9) + (Queen bonus 3 pts, if covered and pre-board score < 21).
 * - Loser gets 0 points for that board.
 * - Match win condition:
 *     1. First player/team to reach targetScore (default ICF is 25 points, or customized 15 points).
 *     2. OR whoever leads after maxBoards (default ICF is 8 boards, or customized 4/6 boards).
 */
@Component
public class CarromScoringEngine implements ScoringEngine {

    @Override
    public String getSportType() {
        return "CARROM";
    }

    @Override
    public void validateScoreEvent(SportsTournamentMatch match, SportsGenericScoreRequest req) {
        // Carrom points in a board typically range from 1 to 12
    }

    @Override
    public boolean checkPeriodWon(SportsTournamentMatch match, SportsMatchPeriodScore period, SportsScoringConfig config) {
        if (period == null) return false;
        int a = period.getScoreTeamA();
        int b = period.getScoreTeamB();
        return a > 0 || b > 0;
    }

    @Override
    public boolean checkMatchWon(SportsTournamentMatch match, List<SportsMatchPeriodScore> periods, SportsScoringConfig config) {
        if (config == null || periods == null || periods.isEmpty()) return false;

        int totalA = periods.stream().mapToInt(SportsMatchPeriodScore::getScoreTeamA).sum();
        int totalB = periods.stream().mapToInt(SportsMatchPeriodScore::getScoreTeamB).sum();

        // 1. Target Score reached (ICF standard is 25 points, fast format is 15 points)
        int targetScore = config.getTargetScore() != null && config.getTargetScore() > 0 ? config.getTargetScore() : 25;
        if (totalA >= targetScore || totalB >= targetScore) {
            return totalA != totalB;
        }

        // 2. Max Boards limit reached (ICF standard is 8 boards)
        int maxBoards = config.getPeriodsCount() != null && config.getPeriodsCount() > 0 ? config.getPeriodsCount() : 8;
        long completedBoards = periods.stream().filter(p -> p.getScoreTeamA() > 0 || p.getScoreTeamB() > 0).count();
        if (completedBoards >= maxBoards) {
            return totalA != totalB;
        }

        return false;
    }

    @Override
    public String formatScoreTeamA(List<SportsMatchPeriodScore> periods) {
        if (periods == null || periods.isEmpty()) return "0";
        int totalA = periods.stream().mapToInt(SportsMatchPeriodScore::getScoreTeamA).sum();
        return String.valueOf(totalA);
    }

    @Override
    public String formatScoreTeamB(List<SportsMatchPeriodScore> periods) {
        if (periods == null || periods.isEmpty()) return "0";
        int totalB = periods.stream().mapToInt(SportsMatchPeriodScore::getScoreTeamB).sum();
        return String.valueOf(totalB);
    }
}
