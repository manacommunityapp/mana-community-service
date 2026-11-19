package com.manacommunity.api.unit.service;

import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.sports.dto.SportsMatchResponse;
import com.manacommunity.api.sports.dto.SportsMatchResultRequest;
import com.manacommunity.api.sports.model.MatchRound;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.scheduler.SportsBracketGenerator;
import com.manacommunity.api.sports.scheduler.SportsTournamentConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import com.manacommunity.api.sports.scheduler.SportsTournamentSchedulerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SportsWinnerAdvancementTest - Winner advancement and placeholder resolution")
class SportsWinnerAdvancementTest {

    @Mock SportsTournamentConfigRepository configRepo;
    @Mock SportsTournamentMatchRepository matchRepo;
    @Mock SportsTournamentGroupRepository groupRepo;
    @Mock SportsGroupTeamStandingRepository standingRepo;
    @Mock SportsAuctionTeamRepository teamRepo;
    @Mock SportsTournamentRepository tournamentRepo;
    @Mock SportsVenueRepository venueRepo;
    @Mock SportsCourtRepository courtRepo;
    @Mock AuditService auditService;

    @InjectMocks SportsBracketGenerator bracketGenerator;
    @InjectMocks SportsTournamentSchedulerService schedulerService;

    @Test
    @DisplayName("bracketGenerator.applyResult advances winner into downstream match with Round of 32 Match 6 (Winner) placeholder")
    void testAdvanceWinnerIntoDownstreamMatch() {
        SportsTournamentConfig config = new SportsTournamentConfig();
        config.setId(10L);

        SportsAuctionTeam teamA = SportsAuctionTeam.builder().id(101L).teamName("Team Alpha").build();
        SportsAuctionTeam teamB = SportsAuctionTeam.builder().id(102L).teamName("Team Beta").build();

        SportsTournamentMatch feederMatch = SportsTournamentMatch.builder()
                .id(106L)
                .config(config)
                .round(MatchRound.ROUND_OF_32)
                .matchNumber(6)
                .teamA(teamA)
                .teamB(teamB)
                .winnerAdvancesToMatchId(203L)
                .scheduledAt(LocalDateTime.now())
                .status(MatchStatus.SCHEDULED)
                .build();

        SportsTournamentMatch downstreamMatch = SportsTournamentMatch.builder()
                .id(203L)
                .config(config)
                .round(MatchRound.ROUND_OF_16)
                .matchNumber(3)
                .winnerFeedFromMatchA(106L)
                .placeholderTeamA("Round of 32 Match 6 (Winner)")
                .placeholderTeamB("Round of 32 Match 7 (Winner)")
                .matchNotes("{\"homeName\":\"Round of 32 Match 6 (Winner)\",\"awayName\":\"Round of 32 Match 7 (Winner)\",\"groupName\":\"ROUND_OF_16\"}")
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .status(MatchStatus.SCHEDULED)
                .build();

        when(matchRepo.findById(106L)).thenReturn(Optional.of(feederMatch));
        when(teamRepo.findById(101L)).thenReturn(Optional.of(teamA));
        when(matchRepo.findById(203L)).thenReturn(Optional.of(downstreamMatch));
        when(matchRepo.save(any(SportsTournamentMatch.class))).thenAnswer(inv -> inv.getArgument(0));

        SportsMatchResultRequest request = new SportsMatchResultRequest(
                106L, 101L, "2", "0", "Played well", null, null, null, null, "Team Alpha", "A"
        );

        bracketGenerator.applyResult(request);

        assertThat(feederMatch.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(feederMatch.getWinner()).isEqualTo(teamA);
        assertThat(feederMatch.getWinnerName()).isEqualTo("Team Alpha");

        assertThat(downstreamMatch.getTeamA()).isEqualTo(teamA);
        assertThat(downstreamMatch.getPlaceholderTeamA()).isEqualTo("Team Alpha");
        assertThat(downstreamMatch.getMatchNotes()).contains("\"homeName\":\"Team Alpha\"");
    }

    @Test
    @DisplayName("schedulerService.toMatchResponse resolves feeder winner when downstream match has placeholder")
    void testToMatchResponseResolvesFeederWinner() {
        SportsAuctionTeam teamAlpha = SportsAuctionTeam.builder().id(101L).teamName("Team Alpha").build();

        SportsTournamentMatch feederMatch = SportsTournamentMatch.builder()
                .id(106L)
                .round(MatchRound.ROUND_OF_32)
                .matchNumber(6)
                .teamA(teamAlpha)
                .winner(teamAlpha)
                .winnerName("Team Alpha")
                .status(MatchStatus.COMPLETED)
                .build();

        SportsTournamentMatch downstreamMatch = SportsTournamentMatch.builder()
                .id(203L)
                .round(MatchRound.ROUND_OF_16)
                .matchNumber(3)
                .winnerFeedFromMatchA(106L)
                .placeholderTeamA("Round of 32 Match 6 (Winner)")
                .placeholderTeamB("Round of 32 Match 7 (Winner)")
                .matchNotes("{\"homeName\":\"Round of 32 Match 6 (Winner)\",\"awayName\":\"Round of 32 Match 7 (Winner)\",\"groupName\":\"ROUND_OF_16\"}")
                .scheduledAt(LocalDateTime.of(2026, 11, 20, 10, 0))
                .status(MatchStatus.SCHEDULED)
                .build();

        when(matchRepo.findById(106L)).thenReturn(Optional.of(feederMatch));

        SportsMatchResponse response = schedulerService.toMatchResponse(downstreamMatch);

        assertThat(response.teamAName()).isEqualTo("Team Alpha");
        assertThat(response.teamBName()).isEqualTo("Round of 32 Match 7 (Winner)");
    }
}
