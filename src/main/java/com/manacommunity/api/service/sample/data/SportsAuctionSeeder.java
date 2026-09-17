package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SportsAuctionSeeder — Seeds tournament auction setups, categories, dispute committees, teams, and queues auction players.
 * Checks if test auction data already exists; if found, cleans and recreates it fresh.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsAuctionSeeder {

    private final SportsAuctionConfigRepository configRepo;
    private final SportsAuctionConfigCategoryRepository configCategoryRepo;
    private final SportsAuctionDisputeCommitteeRepository committeeRepo;
    private final SportsAuctionTeamRepository teamRepo;
    private final SportsAuctionPlayerRepository playerRepo;
    private final SportsAuctionBidRepository bidRepo;
    private final SportsAuctionSessionLogRepository sessionLogRepo;
    private final SportsEventRegistrationRepository regRepo;

    private final SportsMetaSeeder sportsMetaSeeder;
    private final SportsEventSeeder sportsEventSeeder;
    private final UserSeeder userSeeder;

    @Transactional
    public void seed() {
        log.info("Seeding auction configurations (delete & recreate if exists)...");

        SportsMeta cricket = sportsMetaSeeder.getOrCreateSport("Cricket", "🏏");
        SportsEvent summerCup = sportsEventSeeder.getSummerCup();
        AppUser superAdmin = userSeeder.getSuperAdmin();
        AppUser sunil = userSeeder.getSunil();
        AppUser ramesh = userSeeder.getRamesh();
        AppUser user1 = userSeeder.getVarshitha();
        AppUser user6 = userSeeder.getUserByEmail("vikram.singh@gmail.com");
        AppUser user8 = userSeeder.getUserByEmail("rohit.verma@gmail.com");

        // 1. If auction config already exists for this event, delete it and all child records
        deleteAuctionDataForEvent(summerCup.getId());

        // 2. Create Auction Configuration fresh
        SportsAuctionConfig auctionConfig = configRepo.save(SportsAuctionConfig.builder()
                .sport(cricket)
                .event(summerCup)
                .community(summerCup != null ? summerCup.getCommunity() : null)
                .seasonName("Season 2026")
                .auctionFormat(SportsAuctionConfig.AuctionFormat.OPEN_AUCTION)
                .totalTeams(6)
                .totalPlayers(30)
                .budgetPerTeam(100000L)
                .basePrice(1000)
                .bidIncrementDefault(1000)
                .bidIncrementThreshold(10000L)
                .bidIncrementAbove(5000)
                .bidTimerSeconds(30)
                .rtmEnabled(true)
                .unsoldRule(SportsAuctionConfig.UnsoldRule.ROTATION_AUCTION)
                .status(SportsAuctionConfig.AuctionStatus.DRAFT)
                .createdBy(superAdmin)
                .build());

        log.info("✓ Auction config seeded: Season 2026 (id={})", auctionConfig.getId());

        // 3. Create Auction Categories
        createConfigCategory(auctionConfig, "Batsmen");
        createConfigCategory(auctionConfig, "Bowler");
        createConfigCategory(auctionConfig, "All-rounder");
        createConfigCategory(auctionConfig, "Wicket Keeper");

        log.info("✓ Auction config categories seeded: Batsmen, Bowlers, All-rounders, Wicket Keepers");

        // 4. Create Dispute Committee Member
        createCommitteeMember(auctionConfig, "Sunil Kanthala", null, "COMMITTEE_MEMBER");

        log.info("✓ Dispute committee seeded: 1 member");

        // 5. Create Auction Teams
        createAuctionTeam(auctionConfig, "Team 1", sunil, "", 100000L, true, true, summerCup.getId());
        createAuctionTeam(auctionConfig, "Team 2", ramesh, "", 100000L, true, true, summerCup.getId());
        createAuctionTeam(auctionConfig, "Team 3", user1, "", 100000L, true, true, summerCup.getId());
        createAuctionTeam(auctionConfig, "Team 4", user6, "", 100000L, true, true, summerCup.getId());
        createAuctionTeam(auctionConfig, "Team 5", user8, "", 100000L, true, true, summerCup.getId());

        log.info("✓ Auction teams seeded: 5 active teams");

        // 6. Create Auction Players from Confirmed Registrations
        createAuctionPlayersFromConfirmedRegistrations(summerCup, auctionConfig);

        log.info("✓ Auction players seeded from confirmed event registrations");
    }

    public void deleteAuctionDataForEvent(Long eventId) {
        if (eventId == null) return;
        configRepo.findByEventId(eventId).ifPresent(config -> {
            Long configId = config.getId();
            try {
                // Delete bids
                List<SportsAuctionPlayer> players = playerRepo.findByConfigId(configId);
                for (SportsAuctionPlayer p : players) {
                    List<SportsAuctionBid> bids = bidRepo.findByPlayerIdOrderByBidAtDesc(p.getId());
                    if (!bids.isEmpty()) {
                        bidRepo.deleteAll(bids);
                    }
                }
                if (!players.isEmpty()) {
                    playerRepo.deleteAll(players);
                    playerRepo.flush();
                }

                // Delete session logs
                List<SportsAuctionSessionLog> logs = sessionLogRepo.findAll().stream()
                        .filter(l -> l.getConfig() != null && l.getConfig().getId().equals(configId))
                        .toList();
                if (!logs.isEmpty()) {
                    sessionLogRepo.deleteAll(logs);
                    sessionLogRepo.flush();
                }

                // Delete teams
                List<SportsAuctionTeam> teams = teamRepo.findByConfigIdOrderByTeamName(configId);
                if (!teams.isEmpty()) {
                    teamRepo.deleteAll(teams);
                    teamRepo.flush();
                }

                // Delete committee members
                List<SportsAuctionDisputeCommittee> committee = committeeRepo.findAll().stream()
                        .filter(c -> c.getConfig() != null && c.getConfig().getId().equals(configId))
                        .toList();
                if (!committee.isEmpty()) {
                    committeeRepo.deleteAll(committee);
                    committeeRepo.flush();
                }

                // Delete categories
                List<SportsAuctionConfigCategory> categories = configCategoryRepo.findAll().stream()
                        .filter(c -> c.getConfig() != null && c.getConfig().getId().equals(configId))
                        .toList();
                if (!categories.isEmpty()) {
                    configCategoryRepo.deleteAll(categories);
                    configCategoryRepo.flush();
                }

                configRepo.delete(config);
                configRepo.flush();
                log.info("✓ Cleaned existing auction config (id={}) for event id={}", configId, eventId);
            } catch (Exception e) {
                log.warn("Notice while cleaning auction data for event id {}: {}", eventId, e.getMessage());
            }
        });
    }

    private void createConfigCategory(SportsAuctionConfig config, String categoryName) {
        configCategoryRepo.save(new SportsAuctionConfigCategory(config, categoryName));
    }

    private void createCommitteeMember(SportsAuctionConfig config, String memberName,
                                       AppUser user, String role) {
        committeeRepo.save(SportsAuctionDisputeCommittee.builder()
                .config(config)
                .memberName(memberName)
                .user(user)
                .role(role)
                .build());
    }

    private void createAuctionTeam(SportsAuctionConfig config, String teamName, AppUser owner,
                                   String colorHex, long budget,
                                   boolean captainNomination, boolean captainConfirmation, Long eventId) {
        teamRepo.save(SportsAuctionTeam.builder()
                .config(config)
                .community(config.getCommunity() != null ? config.getCommunity() : (config.getEvent() != null ? config.getEvent().getCommunity() : null))
                .event(config.getEvent())
                .teamName(teamName)
                .captainUser(owner)
                .ownerUser(owner)
                .ownerName(owner != null ? owner.getFullName() : null)
                .colorHex(colorHex)
                .totalBudget(budget)
                .remainingBudget(budget)
                .spent(0L)
                .eventId(eventId)
                .captainNomination(captainNomination)
                .captainConfirmation(captainConfirmation)
                .build());
    }

    private void createAuctionPlayersFromConfirmedRegistrations(SportsEvent event, SportsAuctionConfig config) {
        List<SportsEventRegistration> confirmedRegs = regRepo.findByEventIdAndStatus(event.getId(), SportsEventRegistration.RegistrationStatus.CONFIRMED);
        int queueOrder = 1;
        for (SportsEventRegistration reg : confirmedRegs) {
            String cat = "Batsmen";
            if ("Bowler".equalsIgnoreCase(reg.getRole())) cat = "Bowler";
            else if ("All-rounder".equalsIgnoreCase(reg.getRole())) cat = "All-rounder";
            else if ("Wicket Keeper".equalsIgnoreCase(reg.getRole())) cat = "Wicket Keeper";

            SportsAuctionPlayer player = SportsAuctionPlayer.builder()
                    .config(config)
                    .community(config.getCommunity() != null ? config.getCommunity() : (event != null ? event.getCommunity() : null))
                    .user(reg.getUser())
                    .playerName(reg.getPlayerName())
                    .category(cat)
                    .playerRole(reg.getRole())
                    .age(reg.getAge())
                    .basePrice(config.getBasePrice() != null ? config.getBasePrice() : 1000)
                    .statsJson("{\"matches\":24,\"runs\":620,\"wickets\":18}")
                    .queueOrder(queueOrder++)
                    .status(SportsAuctionPlayer.PlayerStatus.QUEUED)
                    .uploadedAt(LocalDateTime.now())
                    .build();
            playerRepo.save(player);
        }
    }
}
