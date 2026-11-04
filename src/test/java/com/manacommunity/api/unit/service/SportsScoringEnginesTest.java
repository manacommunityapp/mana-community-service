package com.manacommunity.api.unit.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.sports.dto.SportsGenericScoreRequest;
import com.manacommunity.api.sports.engine.BadmintonScoringEngine;
import com.manacommunity.api.sports.engine.FootballScoringEngine;
import com.manacommunity.api.sports.engine.TennisScoringEngine;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.scheduler.SportsMatchPeriodScore;
import com.manacommunity.api.sports.scheduler.SportsScoringConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Sports Scoring Engines Tests")
class SportsScoringEnginesTest {

    private BadmintonScoringEngine badmintonEngine;
    private TennisScoringEngine tennisEngine;
    private FootballScoringEngine footballEngine;

    private SportsTournamentMatch match;

    @BeforeEach
    void setUp() {
        badmintonEngine = new BadmintonScoringEngine();
        tennisEngine = new TennisScoringEngine();
        footballEngine = new FootballScoringEngine();

        match = SportsTournamentMatch.builder()
                .id(1L)
                .teamA(SportsAuctionTeam.builder().id(10L).teamName("Team Red").build())
                .teamB(SportsAuctionTeam.builder().id(20L).teamName("Team Blue").build())
                .build();
    }

    @Test
    @DisplayName("Badminton: 21-19 wins period, 20-20 requires 2-point lead, 30-29 caps at 30")
    void badmintonPeriodRules() {
        SportsScoringConfig config = SportsScoringConfig.builder()
                .pointsToWinPeriod(21)
                .mustWinByTwo(true)
                .tiebreakPointsToWin(30)
                .periodsToWin(2)
                .build();

        // 21-19 is a win (reached 21, lead by 2)
        SportsMatchPeriodScore p1 = SportsMatchPeriodScore.builder().scoreTeamA(21).scoreTeamB(19).build();
        assertThat(badmintonEngine.checkPeriodWon(match, p1, config)).isTrue();

        // 21-20 is NOT a win yet (need 2 point lead)
        SportsMatchPeriodScore p2 = SportsMatchPeriodScore.builder().scoreTeamA(21).scoreTeamB(20).build();
        assertThat(badmintonEngine.checkPeriodWon(match, p2, config)).isFalse();

        // 22-20 is a win
        SportsMatchPeriodScore p3 = SportsMatchPeriodScore.builder().scoreTeamA(22).scoreTeamB(20).build();
        assertThat(badmintonEngine.checkPeriodWon(match, p3, config)).isTrue();

        // 30-29 is a win due to max 30 point cap
        SportsMatchPeriodScore p4 = SportsMatchPeriodScore.builder().scoreTeamA(30).scoreTeamB(29).build();
        assertThat(badmintonEngine.checkPeriodWon(match, p4, config)).isTrue();
    }

    @Test
    @DisplayName("Badminton: Service court is RIGHT on even score, LEFT on odd score")
    void badmintonServiceCourt() {
        assertThat(badmintonEngine.getServiceCourt(0)).isEqualTo("RIGHT");
        assertThat(badmintonEngine.getServiceCourt(1)).isEqualTo("LEFT");
        assertThat(badmintonEngine.getServiceCourt(4)).isEqualTo("RIGHT");
        assertThat(badmintonEngine.getServiceCourt(15)).isEqualTo("LEFT");
    }

    @Test
    @DisplayName("Tennis: Set is won at 6-4, 7-5, or 7-6 tiebreak")
    void tennisSetRules() {
        SportsScoringConfig config = SportsScoringConfig.builder()
                .pointsToWinPeriod(6)
                .periodsToWin(2)
                .tiebreakPointsToWin(7)
                .build();

        // 6-4 wins set
        SportsMatchPeriodScore set1 = SportsMatchPeriodScore.builder().scoreTeamA(6).scoreTeamB(4).build();
        assertThat(tennisEngine.checkPeriodWon(match, set1, config)).isTrue();

        // 6-5 does NOT win set (need 2 game lead)
        SportsMatchPeriodScore set2 = SportsMatchPeriodScore.builder().scoreTeamA(6).scoreTeamB(5).build();
        assertThat(tennisEngine.checkPeriodWon(match, set2, config)).isFalse();

        // 7-5 wins set
        SportsMatchPeriodScore set3 = SportsMatchPeriodScore.builder().scoreTeamA(7).scoreTeamB(5).build();
        assertThat(tennisEngine.checkPeriodWon(match, set3, config)).isTrue();

        // 7-6 wins set (tiebreak)
        SportsMatchPeriodScore set4 = SportsMatchPeriodScore.builder().scoreTeamA(7).scoreTeamB(6).build();
        assertThat(tennisEngine.checkPeriodWon(match, set4, config)).isTrue();
    }

    @Test
    @DisplayName("Football: validates valid events and rejects illegal event types")
    void footballEventValidation() {
        SportsGenericScoreRequest validReq = new SportsGenericScoreRequest(
                1L, 10L, 5L, "GOAL", 1, 35, 1, "Header goal", null
        );
        badmintonEngine.validateScoreEvent(match, validReq);

        SportsGenericScoreRequest invalidReq = new SportsGenericScoreRequest(
                1L, 10L, 5L, "SIXER", 1, 35, 6, "Not a football event", null
        );
        assertThatThrownBy(() -> footballEngine.validateScoreEvent(match, invalidReq))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Invalid football event type: SIXER");
    }
}
