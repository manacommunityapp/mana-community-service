package com.manacommunity.api.sports.scheduler;

import com.manacommunity.api.sports.model.MatchRound;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.model.TournamentType;
import com.manacommunity.api.sports.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TournamentStageService Tests")
class TournamentStageServiceTest {

    @Mock private SportsTournamentStageRepository stageRepo;
    @Mock private SportsScheduleVersionRepository versionRepo;
    @Mock private SportsMatchPostponementRepository postponementRepo;
    @Mock private SportsTournamentReserveDayRepository reserveDayRepo;
    @Mock private SportsTournamentConfigRepository configRepo;
    @Mock private SportsTournamentMatchRepository matchRepo;
    @Mock private SportsBracketGenerator bracketGenerator;
    @Mock private SportsAuctionTeamRepository teamRepo;
    @Mock private SportsAuctionConfigRepository auctionConfigRepo;
    @Mock private SportsMatchPersistenceService matchPersistence;

    @InjectMocks
    private TournamentStageService stageService;

    private SportsTournamentConfig testConfig;
    private SportsTournamentStage testStage;

    @BeforeEach
    void setUp() {
        testConfig = SportsTournamentConfig.builder()
                .id(49L)
                .tournamentName("Test Knockout")
                .tournamentType(TournamentType.KNOCKOUT_SINGLE)
                .totalTeams(16)
                .status(SportsTournamentConfig.TournamentStatus.ACTIVE)
                .build();

        testStage = SportsTournamentStage.builder()
                .id(10L)
                .config(testConfig)
                .stageName("Quarter-finals")
                .stageType(SportsTournamentStage.StageType.QUARTER_FINAL)
                .stageOrder(2)
                .stageStatus(SportsTournamentStage.StageStatus.NOT_GENERATED)
                .schedulingMode(SportsTournamentStage.SchedulingMode.HYBRID)
                .build();
    }

    @Test
    @DisplayName("generateStageSchedule: matches linked successfully by candidate rounds")
    void generateStageSchedule_candidateRoundsMatch() {
        when(stageRepo.findById(10L)).thenReturn(Optional.of(testStage));
        when(matchRepo.findByStageId(10L)).thenReturn(Collections.emptyList());
        when(matchRepo.countByConfigId(49L)).thenReturn(8L);

        SportsTournamentMatch m1 = SportsTournamentMatch.builder().id(101L).round(MatchRound.QUARTER_FINAL).roundNumber(2).build();
        SportsTournamentMatch m2 = SportsTournamentMatch.builder().id(102L).round(MatchRound.QUARTER_FINAL).roundNumber(2).build();

        when(matchRepo.findUnassignedByConfigIdAndRounds(eq(49L), anyCollection()))
                .thenReturn(List.of(m1, m2));
        when(versionRepo.findMaxVersionNumber(49L)).thenReturn(0);

        TournamentStageService.GenerateResult result = stageService.generateStageSchedule(10L, null);

        assertThat(result.matchCount()).isEqualTo(2);
        assertThat(result.conflicts()).isEmpty();
        assertThat(m1.getStageId()).isEqualTo(10L);
        assertThat(m2.getStageId()).isEqualTo(10L);
        assertThat(testStage.getStageStatus()).isEqualTo(SportsTournamentStage.StageStatus.DRAFT);
        verify(matchRepo).saveAll(anyList());
    }

    @Test
    @DisplayName("generateStageSchedule: re-generating DRAFT stage re-links existing matches without returning 0")
    void generateStageSchedule_draftStageRetainsAssignedMatches() {
        testStage.setStageStatus(SportsTournamentStage.StageStatus.DRAFT);
        when(stageRepo.findById(10L)).thenReturn(Optional.of(testStage));

        SportsTournamentMatch existingMatch = SportsTournamentMatch.builder()
                .id(201L)
                .stageId(10L)
                .round(MatchRound.QUARTER_FINAL)
                .build();

        when(matchRepo.findByStageId(10L)).thenReturn(List.of(existingMatch));
        when(versionRepo.findMaxVersionNumber(49L)).thenReturn(1);

        TournamentStageService.GenerateResult result = stageService.generateStageSchedule(10L, null);

        assertThat(result.matchCount()).isEqualTo(1);
        assertThat(result.conflicts()).isEmpty();
        verify(matchRepo).saveAll(anyList());
    }

    @Test
    @DisplayName("generateStageSchedule: stage name heuristic matches semi-finals even when stageType is GROUP_STAGE")
    void generateStageSchedule_nameHeuristicFallback() {
        SportsTournamentStage misconfiguredStage = SportsTournamentStage.builder()
                .id(11L)
                .config(testConfig)
                .stageName("Semi-Finals Stage")
                .stageType(SportsTournamentStage.StageType.GROUP_STAGE)
                .stageOrder(3)
                .stageStatus(SportsTournamentStage.StageStatus.NOT_GENERATED)
                .build();

        when(stageRepo.findById(11L)).thenReturn(Optional.of(misconfiguredStage));
        when(matchRepo.findByStageId(11L)).thenReturn(Collections.emptyList());
        when(matchRepo.countByConfigId(49L)).thenReturn(15L);

        // Candidate rounds for GROUP_STAGE returns empty in a knockout single tournament
        when(matchRepo.findUnassignedByConfigIdAndRounds(eq(49L), eq(List.of(MatchRound.GROUP_STAGE, MatchRound.LEAGUE_MATCH))))
                .thenReturn(Collections.emptyList());

        SportsTournamentMatch sf1 = SportsTournamentMatch.builder().id(301L).round(MatchRound.SEMI_FINAL).roundNumber(3).build();
        SportsTournamentMatch sf2 = SportsTournamentMatch.builder().id(302L).round(MatchRound.SEMI_FINAL).roundNumber(3).build();

        // Heuristic recognizes "semi" in stageName
        when(matchRepo.findUnassignedByConfigIdAndRounds(eq(49L), eq(List.of(MatchRound.SEMI_FINAL))))
                .thenReturn(List.of(sf1, sf2));
        when(versionRepo.findMaxVersionNumber(49L)).thenReturn(0);

        TournamentStageService.GenerateResult result = stageService.generateStageSchedule(11L, null);

        assertThat(result.matchCount()).isEqualTo(2);
        assertThat(result.conflicts()).isEmpty();
        assertThat(sf1.getStageId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("getStages: resolves config by event ID fallback when config ID is an event ID")
    void getStages_eventIdFallback() {
        when(configRepo.findById(772L)).thenReturn(Optional.empty());
        when(configRepo.findByEventId(772L)).thenReturn(List.of(testConfig));
        when(stageRepo.findByConfigIdOrderByStageOrder(49L)).thenReturn(List.of(testStage));
        when(matchRepo.findByConfigId(49L)).thenReturn(Collections.emptyList());

        List<TournamentStageService.StageResponse> stages = stageService.getStages(772L);

        assertThat(stages).hasSize(1);
        assertThat(stages.get(0).stageName()).isEqualTo("Quarter-finals");
    }
}
