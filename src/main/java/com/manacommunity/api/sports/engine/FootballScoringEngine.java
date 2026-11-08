package com.manacommunity.api.sports.engine;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.sports.dto.SportsGenericScoreRequest;
import com.manacommunity.api.sports.scheduler.SportsMatchPeriodScore;
import com.manacommunity.api.sports.scheduler.SportsScoringConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class FootballScoringEngine implements ScoringEngine {

    private static final Set<String> VALID_FOOTBALL_EVENTS = Set.of(
            "GOAL", "OWN_GOAL", "PENALTY_GOAL",
            "YELLOW_CARD", "RED_CARD", "SUBSTITUTION", "FOUL", "OFFSIDE", "CORNER"
    );

    @Override
    public String getSportType() {
        return "FOOTBALL";
    }

    @Override
    public void validateScoreEvent(SportsTournamentMatch match, SportsGenericScoreRequest req) {
        if (req.eventType() != null && !VALID_FOOTBALL_EVENTS.contains(req.eventType().toUpperCase())) {
            throw new InvalidInputException("Invalid football event type: " + req.eventType());
        }
    }

    @Override
    public boolean checkPeriodWon(SportsTournamentMatch match, SportsMatchPeriodScore period, SportsScoringConfig config) {
        // Football periods (halves) do not finish the match individually
        return false;
    }

    @Override
    public boolean checkMatchWon(SportsTournamentMatch match, List<SportsMatchPeriodScore> periods, SportsScoringConfig config) {
        int targetPeriods = config != null && config.getPeriodsCount() != null ? config.getPeriodsCount() : 2;
        return periods.size() >= targetPeriods;
    }

    @Override
    public String formatScoreTeamA(List<SportsMatchPeriodScore> periods) {
        int total = periods.stream().mapToInt(SportsMatchPeriodScore::getScoreTeamA).sum();
        return String.valueOf(total);
    }

    @Override
    public String formatScoreTeamB(List<SportsMatchPeriodScore> periods) {
        int total = periods.stream().mapToInt(SportsMatchPeriodScore::getScoreTeamB).sum();
        return String.valueOf(total);
    }
}
