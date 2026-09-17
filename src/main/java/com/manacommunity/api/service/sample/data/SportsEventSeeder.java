package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.user.model.AppUser;

import com.manacommunity.api.model.*;
import com.manacommunity.api.model.scheduler.SportsTournamentConfig;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.repository.scheduler.SportsTournamentConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * SportsEventSeeder — Seeds community tournaments and handles user event registrations.
 * Checks if test data already exists; if found, cleans and recreates it fresh.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsEventSeeder {

    private final SportsEventRepository sportsEventRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final SportsNotificationSchedulerRepository notificationSchedulerRepo;
    private final SportsAuctionConfigRepository auctionConfigRepo;
    private final SportsAuctionConfigCategoryRepository auctionConfigCategoryRepo;
    private final SportsAuctionDisputeCommitteeRepository auctionCommitteeRepo;
    private final SportsAuctionTeamRepository auctionTeamRepo;
    private final SportsAuctionPlayerRepository auctionPlayerRepo;
    private final SportsAuctionBidRepository auctionBidRepo;
    private final SportsAuctionSessionLogRepository auctionSessionLogRepo;
    private final SportsTournamentConfigRepository tournamentConfigRepo;

    private final CommunitySeeder communitySeeder;
    private final SportsMetaSeeder sportsMetaSeeder;
    private final VenueSeeder venueSeeder;
    private final UserSeeder userSeeder;
    private final SportsPlayerCategorySeeder playerCategorySeeder;

    @Transactional
    public void seed() {
        log.info("Seeding community sports events (delete & recreate if exists)...");
        
        SportsMeta cricket = sportsMetaSeeder.getOrCreateSport("Cricket", "🏏");
        Community leCommunity = communitySeeder.getLeCommunity();
        Venue leBoxCricket = venueSeeder.getLeBoxCricket();
        AppUser ramesh = userSeeder.getRamesh();
        
        SportsPlayerCategory cricketKids = playerCategorySeeder.getCategoryByName("Cricket Kids (Under-10)");
        SportsPlayerCategory cricketYouth = playerCategorySeeder.getCategoryByName("Cricket Youth (11-19)");
        SportsPlayerCategory cricketMen = playerCategorySeeder.getCategoryByName("Cricket Men (20+)");

        // Clean legacy event if present in DB
        sportsEventRepo.findAll().stream()
                .filter(e -> e.getName() != null && e.getName().equalsIgnoreCase("Annual Summer Cricket Cup"))
                .findFirst()
                .ifPresent(legacy -> {
                    cleanDependentEventData(legacy.getId());
                    if (legacy.getTournament() != null) legacy.setTournament(null);
                    sportsEventRepo.delete(legacy);
                    sportsEventRepo.flush();
                });

        SportsEvent summerCup = getOrCreateSportsEvent(
                "Annual 2026 Cricket Cup",
                true,
                cricket, leCommunity, leBoxCricket, ramesh, Set.of(cricketKids, cricketYouth, cricketMen),
                SportsEvent.EventStatus.REGISTRATION_OPEN,
                List.of("TEAM"),
                SportsEvent.TournamentType.KNOCKOUT,
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 9, 27),
                LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 20),
                100,
                4, 100,
                "ALL",
                LocalDate.of(1900, 1, 1),
                "3,4", // dispute committee: Sunil(3), Ramesh(4)
                true   // auction = true
        );

        log.info("✓ Sports events seeded: Annual 2026 Cricket Cup (id={})", summerCup.getId());

        // ── 2. Badminton Tournament ──────────────────────────────────────
        SportsMeta badminton = sportsMetaSeeder.getOrCreateSport("Badminton", "🏸");
        Venue leBadmintonCourt = venueSeeder.getLeBadmintonCourt();

        SportsPlayerCategory badBoysU12 = playerCategorySeeder.getCategoryByName("Badminton Boys (Under-12)");
        SportsPlayerCategory badGirlsU12 = playerCategorySeeder.getCategoryByName("Badminton Girls (Under-12)");
        SportsPlayerCategory badBoysTeens = playerCategorySeeder.getCategoryByName("Badminton Boys (13-19)");
        SportsPlayerCategory badGirlsTeens = playerCategorySeeder.getCategoryByName("Badminton Girls (13-19)");
        SportsPlayerCategory badMen20_50 = playerCategorySeeder.getCategoryByName("Badminton Men (20-50)");
        SportsPlayerCategory badWomen20_50 = playerCategorySeeder.getCategoryByName("Badminton Women (20-50)");
        SportsPlayerCategory badSeniorMen = playerCategorySeeder.getCategoryByName("Badminton Senior Men (50+)");
        SportsPlayerCategory badSeniorWomen = playerCategorySeeder.getCategoryByName("Badminton Senior Women (50+)");

        SportsEvent badmintonEvent = getOrCreateSportsEvent(
                "Badminton Community Championship",
                true,
                badminton, leCommunity, leBadmintonCourt, ramesh,
                Set.of(badBoysU12, badGirlsU12, badBoysTeens, badGirlsTeens, badMen20_50, badWomen20_50, badSeniorMen, badSeniorWomen),
                SportsEvent.EventStatus.REGISTRATION_OPEN,
                List.of("SINGLES", "DOUBLES"),
                SportsEvent.TournamentType.KNOCKOUT,
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 5, 30),
                64,
                4, 100,
                "ALL",
                LocalDate.of(1900, 1, 1),
                null
        );

        // Keep legacy badminton event name for existing tests/seeds
        SportsPlayerCategory badmintonMenLegacy = playerCategorySeeder.getCategoryByName("Badminton Men (18+)");
        getOrCreateSportsEvent(
                "Badminton — Men's Above 19",
                true,
                badminton, leCommunity, leBadmintonCourt, ramesh, Set.of(badmintonMenLegacy),
                SportsEvent.EventStatus.REGISTRATION_OPEN,
                List.of("SINGLES", "DOUBLES"),
                SportsEvent.TournamentType.KNOCKOUT_SINGLE,
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 6, 6),
                null,
                null,
                60,
                18, 100,
                "MALE",
                LocalDate.of(1900, 1, 1),
                null
        );

        log.info("✓ Sports events seeded: Badminton Championship (id={})", badmintonEvent.getId());

        // ── 3. Chess Tournament ──────────────────────────────────────────
        SportsMeta chess = sportsMetaSeeder.getOrCreateSport("Chess", "♟️");
        Venue clubhouse = venueSeeder.getLeClubhouse();

        SportsPlayerCategory chessBoysU15 = playerCategorySeeder.getCategoryByName("Chess Boys (Under-15)");
        SportsPlayerCategory chessGirlsU15 = playerCategorySeeder.getCategoryByName("Chess Girls (Under-15)");
        SportsPlayerCategory chessMen16 = playerCategorySeeder.getCategoryByName("Chess Men (16+)");
        SportsPlayerCategory chessWomen16 = playerCategorySeeder.getCategoryByName("Chess Women (16+)");

        SportsEvent chessEvent = getOrCreateSportsEvent(
                "Community Chess Championship",
                true,
                chess, leCommunity, clubhouse, ramesh,
                Set.of(chessBoysU15, chessGirlsU15, chessMen16, chessWomen16),
                SportsEvent.EventStatus.REGISTRATION_OPEN,
                List.of("SINGLES"),
                SportsEvent.TournamentType.ROUND_ROBIN,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 12),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 6, 5),
                32,
                4, 100,
                "ALL",
                LocalDate.of(1900, 1, 1),
                null
        );

        log.info("✓ Sports events seeded: Chess Championship (id={})", chessEvent.getId());

        // ── 4. Carroms Tournament ────────────────────────────────────────
        SportsMeta carroms = sportsMetaSeeder.getOrCreateSport("Carrom", "🔴");

        SportsPlayerCategory carromBoysU12 = playerCategorySeeder.getCategoryByName("Carroms Boys (Under-12)");
        SportsPlayerCategory carromGirlsU12 = playerCategorySeeder.getCategoryByName("Carroms Girls (Under-12)");
        SportsPlayerCategory carromBoysTeens = playerCategorySeeder.getCategoryByName("Carroms Boys (13-19)");
        SportsPlayerCategory carromGirlsTeens = playerCategorySeeder.getCategoryByName("Carroms Girls (13-19)");
        SportsPlayerCategory carromMen = playerCategorySeeder.getCategoryByName("Carroms Men (20-50)");
        SportsPlayerCategory carromWomen = playerCategorySeeder.getCategoryByName("Carroms Women (20-50)");

        SportsEvent carromEvent = getOrCreateSportsEvent(
                "Carroms Community Cup",
                true,
                carroms, leCommunity, clubhouse, ramesh,
                Set.of(carromBoysU12, carromGirlsU12, carromBoysTeens, carromGirlsTeens, carromMen, carromWomen),
                SportsEvent.EventStatus.REGISTRATION_OPEN,
                List.of("SINGLES", "DOUBLES"),
                SportsEvent.TournamentType.KNOCKOUT,
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 17),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 10),
                48,
                4, 50,
                "ALL",
                LocalDate.of(1900, 1, 1),
                null
        );

        log.info("✓ Sports events seeded: Carroms Cup (id={})", carromEvent.getId());

        // ── 5. Table Tennis Tournament ───────────────────────────────────
        SportsMeta tableTennis = sportsMetaSeeder.getOrCreateSport("Table Tennis", "🏓");

        SportsPlayerCategory ttBoysU12 = playerCategorySeeder.getCategoryByName("TT Boys (Under-12)");
        SportsPlayerCategory ttGirlsU12 = playerCategorySeeder.getCategoryByName("TT Girls (Under-12)");
        SportsPlayerCategory ttBoysTeens = playerCategorySeeder.getCategoryByName("TT Boys (13-19)");
        SportsPlayerCategory ttGirlsTeens = playerCategorySeeder.getCategoryByName("TT Girls (13-19)");
        SportsPlayerCategory ttMen20 = playerCategorySeeder.getCategoryByName("TT Men (20+)");
        SportsPlayerCategory ttWomen20 = playerCategorySeeder.getCategoryByName("TT Women (20+)");

        SportsEvent ttEvent = getOrCreateSportsEvent(
                "Table Tennis Open Championship",
                true,
                tableTennis, leCommunity, clubhouse, ramesh,
                Set.of(ttBoysU12, ttGirlsU12, ttBoysTeens, ttGirlsTeens, ttMen20, ttWomen20),
                SportsEvent.EventStatus.REGISTRATION_OPEN,
                List.of("SINGLES", "DOUBLES"),
                SportsEvent.TournamentType.KNOCKOUT,
                LocalDate.of(2026, 6, 20),
                LocalDate.of(2026, 6, 22),
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 15),
                32,
                4, 100,
                "ALL",
                LocalDate.of(1900, 1, 1),
                null
        );

        log.info("✓ Sports events seeded: Table Tennis Championship (id={})", ttEvent.getId());

        // ── 6. Volleyball Tournament ─────────────────────────────────────
        SportsMeta volleyball = sportsMetaSeeder.getOrCreateSport("Volleyball", "🏐");
        Venue volleyballCourt = venueSeeder.getLeVolleyballCourt();

        SportsPlayerCategory volleyballMen = playerCategorySeeder.getCategoryByName("Volleyball Men (15+)");

        SportsEvent volleyballEvent = getOrCreateSportsEvent(
                "Volleyball Premier League",
                true,
                volleyball, leCommunity, volleyballCourt, ramesh,
                Set.of(volleyballMen),
                SportsEvent.EventStatus.REGISTRATION_OPEN,
                List.of("TEAM"),
                SportsEvent.TournamentType.ROUND_ROBIN,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 25),
                16,
                15, 100,
                "MALE",
                LocalDate.of(1900, 1, 1),
                null
        );

        log.info("✓ Sports events seeded: Volleyball Premier League (id={})", volleyballEvent.getId());

        // ════════════════════════════════════════════════════════════════════
        // SPORTS EVENT REGISTRATIONS
        // ════════════════════════════════════════════════════════════════════
        createRegistration(summerCup, userSeeder.getSandeep(), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.REGISTERED, "Sandeep Kamarapu", 36, "All-rounder");
        createRegistration(summerCup, userSeeder.getSunil(), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.REGISTERED, "Sunil Kanthala", 36, "Batsman");
        createRegistration(summerCup, ramesh, cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.REGISTERED, "Ramesh Korlakunta", 36, "Bowler");
        createRegistration(summerCup, userSeeder.getVarshitha(), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.REGISTERED, "user1", 36, "Wicket Keeper");

        // Block A
        createRegistration(summerCup, userSeeder.getUserByEmail("rahul.sharma@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rahul Sharma", 38, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("amit.kumar@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Amit Kumar", 41, "Bowler");
        createRegistration(summerCup, userSeeder.getUserByEmail("vikram.singh@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Vikram Singh", 36, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("rohit.verma@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rohit Verma", 44, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("karan.malhotra@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Karan Malhotra", 32, "Bowler");
        createRegistration(summerCup, userSeeder.getUserByEmail("suresh.nair@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Suresh Nair", 48, "Wicket Keeper");

        // Block B
        createRegistration(summerCup, userSeeder.getUserByEmail("rajat.bhatia@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rajat Bhatia", 30, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("deepak.pillai@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Deepak Pillai", 46, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("manish.tiwari@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Manish Tiwari", 39, "Bowler");
        createRegistration(summerCup, userSeeder.getUserByEmail("arjun.kapoor@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Arjun Kapoor", 35, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("tarun.garg@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Tarun Garg", 28, "Wicket Keeper");
        createRegistration(summerCup, userSeeder.getUserByEmail("nitin.das@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Nitin Das", 47, "All-rounder");

        // Block C
        createRegistration(summerCup, userSeeder.getUserByEmail("siddharth.bose@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Siddharth Bose", 33, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("varun.mehta@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Varun Mehta", 38, "Bowler");
        createRegistration(summerCup, userSeeder.getUserByEmail("gourav.pandey@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Gourav Pandey", 31, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("abhishek.mishra@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Abhishek Mishra", 40, "Wicket Keeper");
        createRegistration(summerCup, userSeeder.getUserByEmail("vishal.shetty@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Vishal Shetty", 43, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("prashant.kadam@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Prashant Kadam", 49, "Bowler");

        // Block D
        createRegistration(summerCup, userSeeder.getUserByEmail("harsh.vardhan@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Harsh Vardhan", 35, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("yash.chopra@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Yash Chopra", 44, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("akash.ambani@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Akash Ambani", 36, "Bowler");
        createRegistration(summerCup, userSeeder.getUserByEmail("naveen.kumar@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Naveen Kumar", 41, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("sanjay.dutt@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sanjay Dutt", 51, "Wicket Keeper");
        createRegistration(summerCup, userSeeder.getUserByEmail("mahesh.babu@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Mahesh Babu", 46, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("ajay.devgn@gmail.com"), cricketMen, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Ajay Devgn", 48, "Bowler");

        log.info("✓ Registrations seeded: 31 confirmed players (Men above 18) for Annual 2026 Cricket Cup");
    }

    public SportsEvent getSummerCup() {
        return sportsEventRepo.findAll().stream()
                .filter(e -> e.getName() != null && (e.getName().equalsIgnoreCase("Annual 2026 Cricket Cup") || e.getName().equalsIgnoreCase("Annual Summer Cricket Cup")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Annual 2026 Cricket Cup has not been seeded yet."));
    }

    private SportsEvent getOrCreateSportsEvent(String name, boolean activeStatus, SportsMeta sport, Community community,
                                               Venue venue, AppUser createdBy, Set<SportsPlayerCategory> categories,
                                               SportsEvent.EventStatus status,
                                               List<String> formats,
                                               SportsEvent.TournamentType tournamentType,
                                               LocalDate dateStart, LocalDate dateEnd,
                                               LocalDate regDateStart, LocalDate regDateEnd,
                                               int maxParticipants,
                                               int minAge, int maxAge,
                                               String gender,
                                               LocalDate playersBorn,
                                               String disputeCommitteeIds) {
        return getOrCreateSportsEvent(name, activeStatus, sport, community, venue, createdBy, categories,
                status, formats, tournamentType, dateStart, dateEnd, regDateStart, regDateEnd,
                maxParticipants, minAge, maxAge, gender, playersBorn, disputeCommitteeIds, false);
    }

    private SportsEvent getOrCreateSportsEvent(String name, boolean activeStatus, SportsMeta sport, Community community,
                                               Venue venue, AppUser createdBy, Set<SportsPlayerCategory> categories,
                                               SportsEvent.EventStatus status,
                                               List<String> formats,
                                               SportsEvent.TournamentType tournamentType,
                                               LocalDate dateStart, LocalDate dateEnd,
                                               LocalDate regDateStart, LocalDate regDateEnd,
                                               int maxParticipants,
                                               int minAge, int maxAge,
                                               String gender,
                                               LocalDate playersBorn,
                                               String disputeCommitteeIds,
                                               boolean auction) {
        SportsEvent existing = sportsEventRepo.findAll().stream()
                .filter(e -> e.getName() != null && e.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            cleanDependentEventData(existing.getId());
            if (existing.getTournament() != null) {
                existing.setTournament(null);
            }
            sportsEventRepo.delete(existing);
            sportsEventRepo.flush();
            log.info("✓ Re-creating sports event: {} (cleaned previous id={})", name, existing.getId());
        }

        return sportsEventRepo.save(SportsEvent.builder()
                .name(name)
                .active(activeStatus)
                .sport(sport)
                .community(community)
                .venue(venue)
                .createdBy(createdBy)
                .status(status)
                .format(formats != null ? formats : java.util.Collections.emptyList())
                .tournamentType(tournamentType)
                .registrationDateStart(regDateStart)
                .registrationDateEnd(regDateEnd)
                .eventDateStart(dateStart)
                .eventDateEnd(dateEnd)
                .maxParticipants(maxParticipants)
                .categories(categories)
                .minAge(minAge)
                .maxAge(maxAge)
                .gender(gender)
                .playersBorn(playersBorn)
                .auction(auction)
                .auctionEnabled(auction)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void cleanDependentEventData(Long eventId) {
        if (eventId == null) return;
        try {
            // 1. Delete notifications
            notificationSchedulerRepo.findByEventId(eventId).forEach(notificationSchedulerRepo::delete);
            notificationSchedulerRepo.flush();

            // 2. Delete auction data if any
            auctionConfigRepo.findByEventId(eventId).ifPresent(config -> {
                Long configId = config.getId();
                List<SportsAuctionPlayer> players = auctionPlayerRepo.findByConfigId(configId);
                for (SportsAuctionPlayer p : players) {
                    List<SportsAuctionBid> bids = auctionBidRepo.findByPlayerIdOrderByBidAtDesc(p.getId());
                    if (!bids.isEmpty()) auctionBidRepo.deleteAll(bids);
                }
                if (!players.isEmpty()) auctionPlayerRepo.deleteAll(players);
                auctionPlayerRepo.flush();

                List<SportsAuctionSessionLog> logs = auctionSessionLogRepo.findAll().stream()
                        .filter(l -> l.getConfig() != null && l.getConfig().getId().equals(configId))
                        .toList();
                if (!logs.isEmpty()) auctionSessionLogRepo.deleteAll(logs);

                List<SportsAuctionTeam> teams = auctionTeamRepo.findByConfigIdOrderByTeamName(configId);
                if (!teams.isEmpty()) auctionTeamRepo.deleteAll(teams);
                auctionTeamRepo.flush();

                List<SportsAuctionDisputeCommittee> committee = auctionCommitteeRepo.findAll().stream()
                        .filter(c -> c.getConfig() != null && c.getConfig().getId().equals(configId))
                        .toList();
                if (!committee.isEmpty()) auctionCommitteeRepo.deleteAll(committee);

                List<SportsAuctionConfigCategory> categories = auctionConfigCategoryRepo.findAll().stream()
                        .filter(c -> c.getConfig() != null && c.getConfig().getId().equals(configId))
                        .toList();
                if (!categories.isEmpty()) auctionConfigCategoryRepo.deleteAll(categories);

                auctionConfigRepo.delete(config);
                auctionConfigRepo.flush();
            });

            // 3. Delete tournament scheduler configs if any
            tournamentConfigRepo.findByEventId(eventId).forEach(tc -> {
                tournamentConfigRepo.delete(tc);
                tournamentConfigRepo.flush();
            });

            // 4. Delete event registrations
            List<SportsEventRegistration> existingRegs = regRepo.findByEventId(eventId);
            if (!existingRegs.isEmpty()) {
                regRepo.deleteAll(existingRegs);
                regRepo.flush();
            }
        } catch (Exception e) {
            log.warn("Notice during cleaning dependent event data for event {}: {}", eventId, e.getMessage());
        }
    }

    private void createRegistration(SportsEvent event, AppUser user, SportsPlayerCategory category,
                                    SportsEvent.MatchFormat matchType,
                                    SportsEventRegistration.RegistrationStatus status,
                                    String playerName, int age, String role) {
        String flatNumber = user != null ? (user.getBlock() + " " + user.getFlatNo()) : "";
        regRepo.save(SportsEventRegistration.builder()
                .event(event)
                .user(user)
                .category(category)
                .matchType(matchType)
                .status(status)
                .playerName(playerName)
                .age(age)
                .flatNumber(flatNumber)
                .role(role)
                .registeredAt(LocalDateTime.now())
                .build());
    }
}
