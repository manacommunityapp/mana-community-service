package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.sports.dto.BallEventRequest;
import com.manacommunity.api.sports.dto.BallEventResponse;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.model.SportsAuctionPlayer;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.repository.SportsAuctionPlayerRepository;
import com.manacommunity.api.sports.repository.SportsMatchBallEventRepository;
import com.manacommunity.api.sports.repository.SportsScoringCommandRepository;
import com.manacommunity.api.sports.repository.SportsScoringCorrectionRepository;
import com.manacommunity.api.sports.repository.SportsTournamentMatchRepository;
import com.manacommunity.api.sports.repository.SportsScoringConfigRepository;
import com.manacommunity.api.sports.scheduler.SportsLiveScoringService;
import com.manacommunity.api.sports.scheduler.SportsMatchBallEvent;
import com.manacommunity.api.sports.scheduler.SportsScoringCommand;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import com.manacommunity.api.sports.service.SportsScoringAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SportsLiveScoringService Tests")
class SportsLiveScoringServiceTest {

    @Mock
    private SportsMatchBallEventRepository ballRepo;

    @Mock
    private SportsTournamentMatchRepository matchRepo;

    @Mock
    private SportsAuctionPlayerRepository playerRepo;

    @Mock
    private SportsScoringCommandRepository commandRepo;

    @Mock
    private SportsScoringCorrectionRepository correctionRepo;

    @Mock
    private SportsScoringAuthorizationService authService;

    @Mock
    private SportsScoringConfigRepository scoringConfigRepo;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SportsLiveScoringService liveScoringService;

    private SportsTournamentMatch match;
    private SportsAuctionTeam teamA;
    private SportsAuctionTeam teamB;
    private SportsAuctionPlayer batsman;
    private SportsAuctionPlayer bowler;

    @BeforeEach
    void setUp() {
        teamA = SportsAuctionTeam.builder().id(1L).teamName("Mumbai Warriors").build();
        teamB = SportsAuctionTeam.builder().id(2L).teamName("Chennai Stars").build();

        match = SportsTournamentMatch.builder()
                .id(100L)
                .teamA(teamA)
                .teamB(teamB)
                .status(MatchStatus.LIVE)
                .scoreTeamA("0/0 (0.0 ov)")
                .scoreTeamB("0/0 (0.0 ov)")
                .build();

        batsman = SportsAuctionPlayer.builder().id(10L).playerName("Rohit").build();
        bowler = SportsAuctionPlayer.builder().id(20L).playerName("Bumrah").build();
    }

    @Test
    @DisplayName("Recording first ball updates match score and stores command idempotency record")
    void recordFirstBallSuccess() {
        UUID commandId = UUID.randomUUID();
        BallEventRequest req = new BallEventRequest(
                100L, 1, 10L, null, 20L,
                4, true, false, null, 0,
                false, null, null, null, "Great boundary!", commandId
        );

        when(commandRepo.findByCommandId(commandId)).thenReturn(Optional.empty());
        when(matchRepo.findById(100L)).thenReturn(Optional.of(match));
        when(ballRepo.findByMatchIdAndInningsNumberAndIsUndoneFalseOrderByDeliveryNumber(100L, 1))
                .thenReturn(Collections.emptyList());
        when(playerRepo.findById(10L)).thenReturn(Optional.of(batsman));
        when(playerRepo.findById(20L)).thenReturn(Optional.of(bowler));

        BallEventResponse resp = liveScoringService.recordBall(req, 501L);

        assertThat(resp).isNotNull();
        assertThat(resp.runsScored()).isEqualTo(4);
        assertThat(resp.totalRuns()).isEqualTo(4);
        assertThat(resp.totalOvers()).isEqualTo("0.1");
        assertThat(resp.isBoundary()).isTrue();

        verify(authService).validateScorerForMatch(100L, 501L);
        verify(ballRepo).save(any(SportsMatchBallEvent.class));
        verify(matchRepo).save(match);
        assertThat(match.getScoreTeamA()).isEqualTo("4/0 (0.1 ov)");

        // Verifies idempotency command was stored
        ArgumentCaptor<SportsScoringCommand> cmdCaptor = ArgumentCaptor.forClass(SportsScoringCommand.class);
        verify(commandRepo).save(cmdCaptor.capture());
        assertThat(cmdCaptor.getValue().getCommandId()).isEqualTo(commandId);
        assertThat(cmdCaptor.getValue().getAppliedBy()).isEqualTo(501L);
    }

    @Test
    @DisplayName("Idempotent ball request returns cached snapshot without re-applying")
    void recordBallDuplicateCommandIdReturnsSnapshot() throws Exception {
        UUID commandId = UUID.randomUUID();
        BallEventRequest req = new BallEventRequest(
                100L, 1, 10L, null, 20L,
                6, false, true, null, 0,
                false, null, null, null, "Six!", commandId
        );

        BallEventResponse cachedResponse = new BallEventResponse(
                55L, 100L, 1, 0, 1, 1,
                6, false, true, null, 0,
                false, null, null, null, null, null,
                10L, "Rohit", null, null, 20L, "Bumrah",
                6, 0, "0.1", "Six!", "2026-11-04T12:00:00"
        );

        SportsScoringCommand existingCmd = SportsScoringCommand.builder()
                .commandId(commandId)
                .responseSnapshot(objectMapper.writeValueAsString(cachedResponse))
                .build();

        when(commandRepo.findByCommandId(commandId)).thenReturn(Optional.of(existingCmd));

        BallEventResponse resp = liveScoringService.recordBall(req, 501L);

        assertThat(resp.id()).isEqualTo(55L);
        assertThat(resp.runsScored()).isEqualTo(6);
        assertThat(resp.isSix()).isTrue();

        // No new ball event should be saved
        verify(ballRepo, never()).save(any());
        verify(matchRepo, never()).save(any());
    }

    @Test
    @DisplayName("Undoing the sole ball in an innings resets score to 0/0 (0.0 ov)")
    void undoSoleBallResetsToZero() {
        SportsMatchBallEvent soleBall = SportsMatchBallEvent.builder()
                .id(1L)
                .match(match)
                .inningsNumber(1)
                .deliveryNumber(1)
                .totalRuns(4)
                .totalWickets(0)
                .totalOvers("0.1")
                .batsman(batsman)
                .bowler(bowler)
                .createdBy(501L)
                .build();

        when(ballRepo.findByMatchIdAndInningsNumberAndIsUndoneFalseOrderByDeliveryNumber(100L, 1))
                .thenReturn(new ArrayList<>(List.of(soleBall)));

        BallEventResponse response = liveScoringService.undoLastBall(100L, 1);

        assertThat(response).isNotNull();
        assertThat(soleBall.getIsUndone()).isTrue();
        verify(ballRepo).save(soleBall);

        // Core fix verification: Match score resets to 0/0 (0.0 ov) instead of staying at 4/0
        assertThat(match.getScoreTeamA()).isEqualTo("0/0 (0.0 ov)");
        verify(matchRepo).save(match);
        verify(correctionRepo).save(any());
    }

    @Test
    @DisplayName("Toss logic accurately identifies batting team order")
    void tossDeterminationIdentifiesBattingTeam() {
        // Case: Team B wins toss and elects to BOWL -> Team A bats first
        match.setTossWinner(teamB);
        match.setTossDecision("BOWL");

        assertThat(liveScoringService.isTeamABatting(match, 1)).isTrue();
        assertThat(liveScoringService.isTeamABatting(match, 2)).isFalse();

        // Case: Team B wins toss and elects to BAT -> Team B bats first
        match.setTossDecision("BAT");
        assertThat(liveScoringService.isTeamABatting(match, 1)).isFalse();
        assertThat(liveScoringService.isTeamABatting(match, 2)).isTrue();
    }
}
