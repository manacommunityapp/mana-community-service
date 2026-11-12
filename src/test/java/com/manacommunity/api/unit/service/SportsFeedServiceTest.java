package com.manacommunity.api.unit.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.sports.dto.SportsFeedMatchResponse;
import com.manacommunity.api.sports.dto.SportsMatchResponse;
import com.manacommunity.api.sports.model.MatchRound;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.model.SportsEvent;
import com.manacommunity.api.sports.model.SportsMeta;
import com.manacommunity.api.sports.model.SportsTournament;
import com.manacommunity.api.sports.repository.SportsEventRepository;
import com.manacommunity.api.sports.repository.SportsTournamentConfigRepository;
import com.manacommunity.api.sports.repository.SportsTournamentMatchRepository;
import com.manacommunity.api.sports.repository.SportsTournamentRepository;
import com.manacommunity.api.sports.scheduler.SportsTournamentConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import com.manacommunity.api.sports.scheduler.SportsTournamentSchedulerService;
import com.manacommunity.api.sports.service.SportsFeedService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService.ResolvedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SportsFeedServiceTest {

    @Mock
    private SportsTournamentMatchRepository matchRepo;
    @Mock
    private SportsTournamentConfigRepository configRepo;
    @Mock
    private SportsTournamentRepository tournamentRepo;
    @Mock
    private SportsEventRepository eventRepo;
    @Mock
    private SportsTournamentSchedulerService schedulerService;

    @InjectMocks
    private SportsFeedService feedService;

    private Community testCommunity;
    private AppUser adminUser;
    private ResolvedUser adminCtx;

    @BeforeEach
    void setUp() {
        testCommunity = new Community();
        testCommunity.setId(10L);
        testCommunity.setName("LE Paradise");

        adminUser = new AppUser();
        adminUser.setId(99L);
        adminUser.setCommunity(testCommunity);

        adminCtx = new ResolvedUser(adminUser, false, 10L);
    }

    @Test
    void testSetEventFlag_cascadesToMatchesAndSetsEventShowInFeed() {
        SportsEvent event = SportsEvent.builder()
                .name("Cricket Men")
                .community(testCommunity)
                .build();
        event.setId(1134L);

        SportsTournamentConfig config = SportsTournamentConfig.builder()
                .id(200L)
                .event(event)
                .community(testCommunity)
                .build();

        SportsTournamentMatch match1 = SportsTournamentMatch.builder()
                .id(501L)
                .config(config)
                .status(MatchStatus.LIVE)
                .showInFeed(null)
                .build();

        when(eventRepo.findById(1134L)).thenReturn(Optional.of(event));
        when(configRepo.findByEventId(1134L)).thenReturn(List.of(config));
        when(matchRepo.findByConfigId(200L)).thenReturn(List.of(match1));

        feedService.setEventFlag(1134L, true, true, adminCtx);

        assertThat(event.getShowInFeed()).isTrue();
        assertThat(match1.getShowInFeed()).isTrue();
        verify(eventRepo).save(event);
        verify(matchRepo).saveAll(List.of(match1));
    }

    @Test
    void testGetEventFlag_returnsCurrentEventShowInFeed() {
        SportsEvent event = SportsEvent.builder()
                .name("Cricket Men")
                .community(testCommunity)
                .showInFeed(true)
                .build();
        event.setId(1134L);

        when(eventRepo.findById(1134L)).thenReturn(Optional.of(event));

        Boolean flag = feedService.getEventFlag(1134L);
        assertThat(flag).isTrue();
    }

    @Test
    void testGetLiveFeed_returnsCandidatesAndNormalizesStatus() {
        SportsMeta cricket = new SportsMeta();
        cricket.setName("Cricket");

        SportsTournamentConfig config = SportsTournamentConfig.builder()
                .id(200L)
                .tournamentName("LEPL 2026")
                .sport(cricket)
                .community(testCommunity)
                .status(SportsTournamentConfig.TournamentStatus.ACTIVE)
                .build();

        SportsTournamentMatch match = SportsTournamentMatch.builder()
                .id(777L)
                .config(config)
                .status(MatchStatus.LIVE)
                .scheduledAt(LocalDateTime.now())
                .showInFeed(true)
                .build();

        when(matchRepo.findFeedCandidatesByCommunityId(eq(10L), any(LocalDateTime.class)))
                .thenReturn(List.of(match));

        SportsMatchResponse mockMatchResponse = new SportsMatchResponse(
                777L, "LEAGUE_MATCH #3", 3, 0,
                1L, "Breeze Blasters", 2L, "Imperial Strikers",
                "#333", "#555", LocalDateTime.now().toString(),
                1L, "LE Box Cricket", null, null,
                "LIVE", "52/2 (5.3 ov)", "Pending", null, null, false, "Tower A", "Tower B"
        );
        when(schedulerService.toMatchResponse(match)).thenReturn(mockMatchResponse);

        List<SportsFeedMatchResponse> result = feedService.getLiveFeed(10L, adminCtx);

        assertThat(result).hasSize(1);
        SportsFeedMatchResponse feedMatch = result.get(0);
        assertThat(feedMatch.matchId()).isEqualTo(777L);
        assertThat(feedMatch.teamAName()).isEqualTo("Breeze Blasters");
        assertThat(feedMatch.status()).isEqualTo("LIVE");
    }

    @Test
    void testIsShownInFeed_guardsDraftMatchesAndConfigs() {
        SportsTournamentConfig draftConfig = SportsTournamentConfig.builder()
                .id(201L)
                .status(SportsTournamentConfig.TournamentStatus.DRAFT)
                .build();

        SportsTournamentMatch matchInDraft = SportsTournamentMatch.builder()
                .id(888L)
                .config(draftConfig)
                .status(MatchStatus.LIVE)
                .showInFeed(true)
                .build();

        assertThat(feedService.isShownInFeed(matchInDraft)).isFalse();
    }
}
