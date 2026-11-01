package com.manacommunity.api.sports;

import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.sample.data.SportsTournamentLifecycleSeeder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@ActiveProfiles("local")
class SportsTournamentLifecycleSeederLocalTest {

    @Autowired
    private SportsTournamentLifecycleSeeder seeder;

    @Autowired
    private SportsTournamentRepository tournamentRepo;

    @Autowired
    private SportsTournamentConfigRepository configRepo;

    @Autowired
    private SportsTournamentGroupRepository groupRepo;

    @Autowired
    private SportsTournamentMatchRepository matchRepo;

    @Autowired
    private SportsGroupTeamStandingRepository standingRepo;

    @Autowired
    private SportsMatchResultRepository matchResultRepo;

    @Test
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.test.annotation.Commit
    @DisplayName("Seed complete sports tournament lifecycle in local DB and verify all entities")
    void testSeedSportsTournamentLifecycle() {
        seeder.seed();

        // 1. Verify Tournament
        SportsTournament tournament = tournamentRepo.findAll().stream()
                .filter(t -> SportsTournamentLifecycleSeeder.TOURNAMENT_NAME.equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElse(null);
        assertThat(tournament).isNotNull();
        assertThat(tournament.getRegistrationStatus()).isEqualTo(SportsEventStatus.COMPLETED);

        // 2. Verify Config
        List<SportsTournamentConfig> configs = configRepo.findAll().stream()
                .filter(c -> "LEPL 2026 Fixture Schedule".equalsIgnoreCase(c.getTournamentName()))
                .toList();
        assertThat(configs).isNotEmpty();
        SportsTournamentConfig config = configs.get(0);
        assertThat(config.getStatus()).isEqualTo(SportsTournamentConfig.TournamentStatus.COMPLETED);

        // 3. Verify Groups
        List<SportsTournamentGroup> groups = groupRepo.findByConfigIdOrderByGroupOrder(config.getId());
        assertThat(groups).hasSize(2);

        // 4. Verify Matches (2 Group + 2 Semis + 1 Final + 1 Live = 6 matches)
        List<SportsTournamentMatch> matches = matchRepo.findByConfigId(config.getId());
        assertThat(matches).hasSize(6);

        long completedMatches = matches.stream().filter(m -> m.getStatus() == MatchStatus.COMPLETED).count();
        long liveMatches = matches.stream().filter(m -> m.getStatus() == MatchStatus.LIVE).count();
        assertThat(completedMatches).isEqualTo(5);
        assertThat(liveMatches).isEqualTo(1);

        // 5. Verify Standings
        for (SportsTournamentGroup group : groups) {
            List<SportsGroupTeamStanding> standings = standingRepo.findByGroupIdOrderByPointsDescNetRunRateDesc(group.getId());
            assertThat(standings).hasSize(2);
        }

        // 6. Verify Grand Final and Result
        SportsTournamentMatch grandFinal = matches.stream()
                .filter(m -> m.getRound() == MatchRound.FINAL)
                .findFirst()
                .orElse(null);
        assertThat(grandFinal).isNotNull();
        assertThat(grandFinal.getWinner().getTeamName()).isEqualTo("Emperia Royals");

        SportsMatchResult matchResult = matchResultRepo.findByMatchId(grandFinal.getId()).orElse(null);
        assertThat(matchResult).isNotNull();
        assertThat(matchResult.getWinMargin()).isEqualTo("8 runs");
    }
}
