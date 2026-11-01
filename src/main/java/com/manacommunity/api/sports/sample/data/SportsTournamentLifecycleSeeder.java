package com.manacommunity.api.sports.sample.data;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.service.sample.data.CommunitySeeder;
import com.manacommunity.api.service.sample.data.UserSeeder;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SportsTournamentLifecycleSeeder
 * Seeds complete tournament lifecycle test data from group stage fixtures
 * through live scoring, standings, playoffs, and tournament closure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsTournamentLifecycleSeeder {

    public static final String TOURNAMENT_NAME = "LEPL 2026 — Lakshmi Emperia Premier League";
    public static final String EVENT_NAME = "LEPL 2026 Cricket Championship";

    private final SportsTournamentRepository tournamentRepo;
    private final SportsEventRepository sportsEventRepo;
    private final SportsTournamentConfigRepository tournamentConfigRepo;
    private final SportsTournamentGroupRepository groupRepo;
    private final SportsTournamentMatchRepository matchRepo;
    private final SportsGroupTeamStandingRepository standingRepo;
    private final SportsMatchResultRepository matchResultRepo;
    private final SportsAuctionTeamRepository teamRepo;
    private final SportsAuctionConfigRepository auctionConfigRepo;
    private final SportsVenueRepository venueRepo;
    private final SportsCourtRepository courtRepo;
    private final SportsMetaSeeder sportsMetaSeeder;
    private final SportsVenueSeeder venueSeeder;
    private final CommunitySeeder communitySeeder;
    private final UserSeeder userSeeder;

    @Transactional
    public void seed() {
        log.info("Seeding complete sports tournament lifecycle (from groups to closure)...");

        Community leCommunity = communitySeeder.getLeCommunity();
        AppUser ramesh = userSeeder.getRamesh();
        AppUser superAdmin = userSeeder.getSuperAdmin();

        SportsMeta cricket = sportsMetaSeeder.getOrCreateSport("Cricket", "🏏");
        SportsVenue venue = venueSeeder.getLeBoxCricket();

        SportsCourt court = courtRepo.findByVenueId(venue.getId()).stream()
                .findFirst()
                .orElseGet(() -> courtRepo.save(SportsCourt.builder()
                        .venue(venue)
                        .name("Pitch 1 (Box Turf)")
                        .build()));

        // 1. Clean existing lifecycle data if present
        cleanExistingTournament();

        // 2. Create Completed Parent Tournament
        SportsTournament tournament = tournamentRepo.save(SportsTournament.builder()
                .name(TOURNAMENT_NAME)
                .description("Grand Annual Community Cricket Championship featuring 4 franchise teams across league stage, playoffs, and the grand final.")
                .eventDateStart(LocalDate.now().minusDays(10))
                .eventDateEnd(LocalDate.now().minusDays(1))
                .registrationDateStart(LocalDate.now().minusDays(25))
                .registrationDateEnd(LocalDate.now().minusDays(15))
                .maxParticipants(120)
                .contactName("Ramesh Korlakunta")
                .contactNumber("9876543210")
                .contactEmail("ramesh@gmail.com")
                .allowAdminChat(true)
                .startTime("08:00 AM")
                .dueTime("10:00 PM")
                .otherContacts("[]")
                .bannerImage("")
                .registrationStatus(SportsEventStatus.COMPLETED)
                .sportsEvents(new ArrayList<>())
                .sponsors(new ArrayList<>())
                .community(leCommunity)
                .createdAt(LocalDateTime.now().minusDays(25))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build());

        // 3. Create Sub-Event
        SportsEvent event = SportsEvent.builder()
                .name(EVENT_NAME)
                .tournament(tournament)
                .sport(cricket)
                .community(leCommunity)
                .venue(venue)
                .eventDateStart(LocalDate.now().minusDays(10))
                .eventDateEnd(LocalDate.now().minusDays(1))
                .registrationDateStart(LocalDate.now().minusDays(25))
                .registrationDateEnd(LocalDate.now().minusDays(15))
                .maxParticipants(60)
                .active(false)
                .status(SportsEventStatus.COMPLETED)
                .tournamentType(SportsEvent.TournamentType.ROUND_ROBIN)
                .auctionEnabled(true)
                .minAge(16)
                .maxAge(65)
                .createdAt(LocalDateTime.now().minusDays(25))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        event.setFormat(List.of(SportsEvent.MatchFormat.TEAM));
        event = sportsEventRepo.save(event);

        tournament.getSportsEvents().add(event);
        tournamentRepo.save(tournament);

        // 4. Create Auction Configuration & 4 Teams
        SportsAuctionConfig auctionConfig = auctionConfigRepo.save(SportsAuctionConfig.builder()
                .sport(cricket)
                .event(event)
                .community(leCommunity)
                .seasonName("Season 2026")
                .auctionFormat(SportsAuctionConfig.AuctionFormat.OPEN_AUCTION)
                .totalTeams(4)
                .totalPlayers(44)
                .budgetPerTeam(100000L)
                .basePrice(1000)
                .bidIncrementDefault(1000)
                .bidIncrementThreshold(10000L)
                .bidIncrementAbove(5000)
                .bidTimerSeconds(30)
                .rtmEnabled(true)
                .unsoldRule(SportsAuctionConfig.UnsoldRule.ROTATION_AUCTION)
                .status(SportsAuctionConfig.AuctionStatus.COMPLETED)
                .createdBy(superAdmin)
                .build());

        SportsAuctionTeam teamRoyals = createTeam(auctionConfig, event, leCommunity, "Emperia Royals", "Sandeep Kamarapu", "#2563eb", 100000L, 96000L);
        SportsAuctionTeam teamTitans = createTeam(auctionConfig, event, leCommunity, "Lakshmi Titans", "Sunil Kanthala", "#dc2626", 100000L, 92500L);
        SportsAuctionTeam teamStrikers = createTeam(auctionConfig, event, leCommunity, "Imperial Strikers", "Ramesh Korlakunta", "#16a34a", 100000L, 89000L);
        SportsAuctionTeam teamBlasters = createTeam(auctionConfig, event, leCommunity, "Breeze Blasters", "Vikram Singh", "#d97706", 100000L, 95000L);

        // 5. Create Tournament Scheduler Config & Groups
        SportsTournamentConfig schedConfig = tournamentConfigRepo.save(SportsTournamentConfig.builder()
                .tournamentName("LEPL 2026 Fixture Schedule")
                .sport(cricket)
                .community(leCommunity)
                .event(event)
                .tournamentType(TournamentType.GROUP_KNOCKOUT)
                .totalTeams(4)
                .numberOfGroups(2)
                .teamsPerGroup(2)
                .teamsAdvancingPerGroup(1)
                .thirdPlaceMatch(true)
                .hasSeeding(true)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(1))
                .matchDurationMinutes(90)
                .breakBetweenMatchesMinutes(15)
                .venue(venue)
                .pointsForWin(2)
                .pointsForDraw(1)
                .pointsForLoss(0)
                .status(SportsTournamentConfig.TournamentStatus.COMPLETED)
                .createdBy(ramesh)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build());

        SportsTournamentGroup groupA = groupRepo.save(SportsTournamentGroup.builder()
                .config(schedConfig)
                .groupName("Group A")
                .groupOrder(1)
                .build());

        SportsTournamentGroup groupB = groupRepo.save(SportsTournamentGroup.builder()
                .config(schedConfig)
                .groupName("Group B")
                .groupOrder(2)
                .build());

        // 6. Create Group Stage Standings
        standingRepo.save(SportsGroupTeamStanding.builder()
                .group(groupA).team(teamRoyals).played(1).won(1).lost(0).drawn(0).points(2)
                .netRunRate(1.800).runsFor(98).oversFor(10).runsAgainst(80).oversAgainst(10)
                .qualified(true).eliminated(false).build());

        standingRepo.save(SportsGroupTeamStanding.builder()
                .group(groupA).team(teamStrikers).played(1).won(0).lost(1).drawn(0).points(0)
                .netRunRate(-1.800).runsFor(80).oversFor(10).runsAgainst(98).oversAgainst(10)
                .qualified(false).eliminated(true).build());

        standingRepo.save(SportsGroupTeamStanding.builder()
                .group(groupB).team(teamTitans).played(1).won(1).lost(0).drawn(0).points(2)
                .netRunRate(0.425).runsFor(85).oversFor(10).runsAgainst(84).oversAgainst(10)
                .qualified(true).eliminated(false).build());

        standingRepo.save(SportsGroupTeamStanding.builder()
                .group(groupB).team(teamBlasters).played(1).won(0).lost(1).drawn(0).points(0)
                .netRunRate(-0.425).runsFor(84).oversFor(10).runsAgainst(85).oversAgainst(10)
                .qualified(false).eliminated(true).build());

        // 7. Create Group Stage Matches
        createMatch(schedConfig, leCommunity, groupA, MatchRound.GROUP_STAGE, 1, 1, 1,
                teamRoyals, teamStrikers, LocalDateTime.now().minusDays(8), venue, court,
                MatchStatus.COMPLETED, "98/4 (10.0 ov)", "80/7 (10.0 ov)", teamRoyals,
                ResultType.WIN, "18 runs", "Emperia Royals set a formidable 98 on the board and defended clinically.");

        createMatch(schedConfig, leCommunity, groupB, MatchRound.GROUP_STAGE, 1, 2, 2,
                teamTitans, teamBlasters, LocalDateTime.now().minusDays(7), venue, court,
                MatchStatus.COMPLETED, "85/6 (9.4 ov)", "84/9 (10.0 ov)", teamTitans,
                ResultType.WIN, "4 wickets", "Thrilling last-over chase by Lakshmi Titans to seal top spot in Group B.");

        // 8. Create Playoff Matches: Semi-Finals and Grand Final
        createMatch(schedConfig, leCommunity, null, MatchRound.SEMI_FINAL, 2, 1, 1,
                teamRoyals, teamBlasters, LocalDateTime.now().minusDays(4), venue, court,
                MatchStatus.COMPLETED, "112/3 (10.0 ov)", "91/6 (10.0 ov)", teamRoyals,
                ResultType.WIN, "21 runs", "Royals advanced to the final with a commanding batting display.");

        createMatch(schedConfig, leCommunity, null, MatchRound.SEMI_FINAL, 2, 2, 2,
                teamTitans, teamStrikers, LocalDateTime.now().minusDays(3), venue, court,
                MatchStatus.COMPLETED, "95/5 (9.2 ov)", "94/8 (10.0 ov)", teamTitans,
                ResultType.WIN, "5 wickets", "Titans pulled off a stunning run-chase to secure their ticket to the Grand Final.");

        SportsTournamentMatch grandFinal = createMatch(schedConfig, leCommunity, null, MatchRound.FINAL, 3, 1, 3,
                teamRoyals, teamTitans, LocalDateTime.now().minusDays(1), venue, court,
                MatchStatus.COMPLETED, "124/4 (10.0 ov)", "116/7 (10.0 ov)", teamRoyals,
                ResultType.WIN, "8 runs", "Emperia Royals crowned Champions of LEPL 2026 after a nail-biting final over!");

        // 9. Create Final Match Result Record
        matchResultRepo.save(SportsMatchResult.builder()
                .match(grandFinal)
                .resultType(ResultType.WIN)
                .winMargin("8 runs")
                .matchSummary("Champion: Emperia Royals | Runner-up: Lakshmi Titans | Player of Tournament: Sandeep Kamarapu")
                .build());

        // 10. Also seed a LIVE match for testing the Live Scoring Panel
        SportsTournamentMatch liveMatch = createMatch(schedConfig, leCommunity, groupA, MatchRound.LEAGUE_MATCH, 1, 3, 4,
                teamBlasters, teamStrikers, LocalDateTime.now().minusMinutes(30), venue, court,
                MatchStatus.LIVE, "52/2 (5.3 ov)", "Pending", null,
                null, null, "Exhibition Match in progress for Live Scoring & Referee panel testing.");
        liveMatch.setStartedAt(LocalDateTime.now().minusMinutes(25));
        matchRepo.save(liveMatch);

        log.info("═══════════════════════════════════════════════════════════");
        log.info("✓ Complete sports lifecycle seeded successfully!");
        log.info("  Tournament : {} (id={}) [COMPLETED]", tournament.getName(), tournament.getId());
        log.info("  Config     : {} (id={})", schedConfig.getTournamentName(), schedConfig.getId());
        log.info("  Champion   : {} (Winner of Grand Final id={})", teamRoyals.getTeamName(), grandFinal.getId());
        log.info("  Live Match : {} vs {} (id={}) [LIVE for scoring test]", teamBlasters.getTeamName(), teamStrikers.getTeamName(), liveMatch.getId());
        log.info("═══════════════════════════════════════════════════════════");
    }

    private SportsAuctionTeam createTeam(SportsAuctionConfig config, SportsEvent event, Community community,
                                         String teamName, String ownerName, String colorHex, long budget, long spent) {
        return teamRepo.save(SportsAuctionTeam.builder()
                .config(config)
                .event(event)
                .community(community)
                .teamName(teamName)
                .ownerName(ownerName)
                .colorHex(colorHex)
                .totalBudget(budget)
                .remainingBudget(budget - spent)
                .spent(spent)
                .eventId(event.getId())
                .captainNomination(true)
                .captainConfirmation(true)
                .build());
    }

    private SportsTournamentMatch createMatch(SportsTournamentConfig config, Community community,
                                              SportsTournamentGroup group, MatchRound round,
                                              int roundNumber, int matchNumber, int bracketSlot,
                                              SportsAuctionTeam teamA, SportsAuctionTeam teamB,
                                              LocalDateTime scheduledAt, SportsVenue venue, SportsCourt court,
                                              MatchStatus status, String scoreA, String scoreB,
                                              SportsAuctionTeam winner, ResultType resultType,
                                              String winMargin, String summary) {
        return matchRepo.save(SportsTournamentMatch.builder()
                .config(config)
                .community(community)
                .group(group)
                .round(round)
                .roundNumber(roundNumber)
                .matchNumber(matchNumber)
                .bracketSlot(bracketSlot)
                .teamA(teamA)
                .teamB(teamB)
                .scheduledAt(scheduledAt)
                .durationMinutes(90)
                .venue(venue)
                .court(court)
                .status(status)
                .scoreTeamA(scoreA)
                .scoreTeamB(scoreB)
                .winner(winner)
                .resultType(resultType)
                .winMargin(winMargin)
                .matchSummary(summary)
                .startedAt(status == MatchStatus.COMPLETED ? scheduledAt : null)
                .completedAt(status == MatchStatus.COMPLETED ? scheduledAt.plusMinutes(85) : null)
                .build());
    }

    private void cleanExistingTournament() {
        tournamentRepo.findAll().stream()
                .filter(t -> t.getName() != null && t.getName().equalsIgnoreCase(TOURNAMENT_NAME))
                .findFirst()
                .ifPresent(existing -> {
                    try {
                        List<SportsEvent> events = sportsEventRepo.findByTournamentId(existing.getId());
                        for (SportsEvent ev : events) {
                            tournamentConfigRepo.findByEventId(ev.getId()).forEach(cfg -> {
                                List<SportsTournamentMatch> matches = matchRepo.findByConfigId(cfg.getId());
                                for (SportsTournamentMatch m : matches) {
                                    matchResultRepo.findByMatchId(m.getId()).ifPresent(matchResultRepo::delete);
                                }
                                matchRepo.deleteAll(matches);
                                matchRepo.flush();

                                List<SportsTournamentGroup> groups = groupRepo.findByConfigIdOrderByGroupOrder(cfg.getId());
                                for (SportsTournamentGroup g : groups) {
                                    List<SportsGroupTeamStanding> standings = standingRepo.findByGroupIdOrderByPointsDescNetRunRateDesc(g.getId());
                                    standingRepo.deleteAll(standings);
                                }
                                standingRepo.flush();
                                groupRepo.deleteAll(groups);
                                groupRepo.flush();
                                tournamentConfigRepo.delete(cfg);
                                tournamentConfigRepo.flush();
                            });

                            auctionConfigRepo.findByEventId(ev.getId()).ifPresent(ac -> {
                                List<SportsAuctionTeam> teams = teamRepo.findByConfigIdOrderByTeamName(ac.getId());
                                teamRepo.deleteAll(teams);
                                teamRepo.flush();
                                auctionConfigRepo.delete(ac);
                                auctionConfigRepo.flush();
                            });

                            ev.setTournament(null);
                            sportsEventRepo.save(ev);
                            sportsEventRepo.delete(ev);
                        }
                        tournamentRepo.delete(existing);
                        tournamentRepo.flush();
                        log.info("✓ Cleaned previous tournament lifecycle data: {}", TOURNAMENT_NAME);
                    } catch (Exception e) {
                        log.warn("Notice while cleaning previous lifecycle tournament: {}", e.getMessage());
                    }
                });
    }
}