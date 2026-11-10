package com.manacommunity.api.service.sample;
import com.manacommunity.api.sports.sample.data.*;

import com.manacommunity.api.constants.ModuleConstants;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.service.sample.data.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SampleDataService — Seeds the database with baseline data matching
 * the current production state by orchestrating dedicated seeder services.
 */
@Slf4j
@Service
@Profile("!prod")
@RequiredArgsConstructor
public class SampleDataService implements ApplicationRunner {

    private final jakarta.persistence.EntityManager entityManager;
    private final Environment environment;

    private final RolePermissionSeeder rolePermissionSeeder;
    private final CommunitySeeder communitySeeder;
    private final SportsMetaSeeder sportsMetaSeeder;
    private final UserSeeder userSeeder;
    private final SportsPlayerCategorySeeder playerCategorySeeder;
    private final SportsVenueSeeder venueSeeder;
    private final CommunityLeaderSeeder communityLeaderSeeder;
    private final SportsEventSeeder sportsEventSeeder;
    private final SportsTournamentSeeder tournamentSeeder;
    private final SportsAuctionSeeder auctionSeeder;
    private final InventorySeeder inventorySeeder;

    private final DefaultCommunityModuleDataService defaultCommunityModuleDataService;
    // Dedicated per-table sample seeders
    private final RolePermissionDataSeeder rolePermissionDataSeeder;
    private final SportsVenueDataSeeder venueDataSeeder;
    private final SportsCourtDataSeeder courtDataSeeder;
    private final SportsEventRegistrationDataSeeder sportsEventRegistrationDataSeeder;
    private final SportsTournamentLifecycleSeeder tournamentLifecycleSeeder;
    private final EmailTemplateFeeder emailTemplateFeeder;
    private final com.manacommunity.api.user.repository.AppUserRepository userRepo;
    private final com.manacommunity.api.sports.repository.SportsEventRegistrationRepository regRepo;

    @Override
    public void run(ApplicationArguments args) {
        if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            log.warn("Safety guard: Auto-running SampleDataService is disabled in 'prod' profile. Skipping startup seeding.");
            return;
        }

        long registrationCount = regRepo != null ? regRepo.count() : 0;
        if (registrationCount > 0) {
            log.warn("SAFETY GUARD TRIGGERED: {} user registrations exist in database. " +
                     "Never wiping data or auto-running seeders when user registrations exist in database. Skipping startup seeding.", registrationCount);
            return;
        }

        try {
            entityManager.createNativeQuery("SELECT setval('manacommunity.roles_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM manacommunity.roles), false)").getSingleResult();
            entityManager.createNativeQuery("SELECT setval('manacommunity.app_user_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM manacommunity.app_user), false)").getSingleResult();
            entityManager.createNativeQuery("SELECT setval('manacommunity.sports_event_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM manacommunity.sports_event), false)").getSingleResult();
            entityManager.createNativeQuery("SELECT setval('manacommunity.sports_tournament_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM manacommunity.sports_tournament), false)").getSingleResult();
            entityManager.createNativeQuery("SELECT setval('manacommunity.sports_event_registration_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM manacommunity.sports_event_registration), false)").getSingleResult();
            log.info("✓ Core table sequences (roles, app_user, sports_event, sports_tournament, registrations) synchronized successfully.");
        } catch (Exception e) {
            log.warn("Could not synchronize core sequences on startup: {}", e.getMessage());
        }

        long userCount = userRepo.count();
        log.info("Current AppUser count in database: {}", userCount);
        if (userCount == 0) {
            log.info("AppUser table is empty! Executing default user & community seeding...");
            executeDefaultDataSql();
            userSeeder.seed();
        }

        boolean insertSampleData = environment.getProperty("app.insert-sample-data", Boolean.class, false);
        boolean insertSportsSampleData = environment.getProperty("app.insert-sports-sample-data", Boolean.class, false)
                || environment.getProperty("app.sports.insert-sample-data", Boolean.class, false)
                || environment.getProperty("app.insert-sports-data", Boolean.class, false);

        if (insertSampleData) {
            log.info("Running sample data seeding as app.insert-sample-data is enabled...");
            executeSampleDataSql();
        } else if (insertSportsSampleData) {
            log.info("Running complete sports lifecycle sample data seeding as app.insert-sports-sample-data is enabled...");
            seedSportsLifecycleOnly();
        }
    }

    @Transactional
    public String executeDefaultDataSql() {
        try{
            communitySeeder.defaultSeed();
            rolePermissionSeeder.defaultSeed();
            userSeeder.defaultSeed();

            Community general = communitySeeder.getGeneralCommunity();
            defaultCommunityModuleDataService.seedModulesForCommunity(
                    general.getId(),
                    Set.of(ModuleConstants.MODULE_COMMUNITY_FEED, ModuleConstants.MODULE_ADMIN_HUB));

            sportsMetaSeeder.defaultSeed();
            playerCategorySeeder.defaultSeed();

            communitySeeder.seed();
            emailTemplateFeeder.seed();

            return "Default data successfully seeded using Java repositories!";
        } catch (Exception e) {
            log.error("Failed to seed database: ", e);
            return "Failed to execute sample data: " + e.getMessage();
        }
    }

    @Transactional
    public String executeSampleDataSql() {
        if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            log.warn("Safety guard: executeSampleDataSql cannot run in 'prod' profile.");
            return "Sample data seeding blocked in 'prod' profile.";
        }

        long registrationCount = regRepo != null ? regRepo.count() : 0;
        if (registrationCount > 0) {
            log.warn("SAFETY GUARD TRIGGERED: {} user registrations exist in database. " +
                     "Never wiping data or running sample data seeding when user registrations exist in database.", registrationCount);
            return "Sample data seeding skipped: " + registrationCount + " user registrations exist in the database.";
        }

        try {
            log.info("Starting Java-based database seeding...");

            // ════════════════════════════════════════════════════════════════════
            // Execute modular seeders in strict dependency order
            // ════════════════════════════════════════════════════════════════════

            // Layer 1 — users, permissions, base categories
            userSeeder.seed();
            rolePermissionSeeder.seedUserPermissions();
            playerCategorySeeder.seed();

            // Layer 2 — venues and community setup
            venueSeeder.seed();
            Community le = communitySeeder.getLeCommunity();
            defaultCommunityModuleDataService.seedModulesForCommunity(
                    le.getId(),
                    Set.of(ModuleConstants.MODULE_COMMUNITY_FEED,
                           ModuleConstants.MODULE_SPORTS,
                           ModuleConstants.MODULE_EVENTS,
                           ModuleConstants.MODULE_ADMIN_HUB));
            communityLeaderSeeder.seed();

            // Layer 3 — sports events + registrations (depends on users, venues, player categories)
            sportsEventSeeder.seed();

            // Layer 4 — tournaments (depends on sports events)
            tournamentSeeder.seed();

            // Layer 5 — auction (depends on sports events and users)
            auctionSeeder.seed();

            // Layer 6 — dedicated per-table seeders (depend on previous layers)
            courtDataSeeder.seed();
            sportsEventRegistrationDataSeeder.seed();

            // Layer 7 — complete tournament lifecycle (fixtures, standings, playoffs, final, closure)
            tournamentLifecycleSeeder.seed();

            log.info("═══════════════════════════════════════════════════════════");
            log.info("  Java-based database seeding completed successfully!");
            log.info("═══════════════════════════════════════════════════════════");
            return "Sample data successfully seeded using Java repositories!";

        } catch (Exception e) {
            log.error("Failed to seed database: ", e);
            return "Failed to execute sample data: " + e.getMessage();
        }
    }

    /**
     * Dedicated seeder method invoked when app.insert-sports-sample-data=true.
     * Seeds base prerequisites and complete sports lifecycle fixtures up to closure.
     */
    @Transactional
    public void seedSportsLifecycleOnly() {
        if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            log.warn("Safety guard: seedSportsLifecycleOnly cannot run in 'prod' profile.");
            return;
        }

        long registrationCount = regRepo != null ? regRepo.count() : 0;
        if (registrationCount > 0) {
            log.warn("SAFETY GUARD TRIGGERED: {} user registrations exist in database. " +
                     "Never wiping data or running sports lifecycle seeding when user registrations exist in database.", registrationCount);
            return;
        }

        try {
            log.info("Starting isolated sports lifecycle sample data seeding...");

            // Prerequisites: base roles, users, venues, sports metadata, community
            communitySeeder.seed();
            userSeeder.seed();
            rolePermissionSeeder.seedUserPermissions();
            playerCategorySeeder.seed();
            sportsMetaSeeder.defaultSeed();
            venueSeeder.seed();

            Community le = communitySeeder.getLeCommunity();
            defaultCommunityModuleDataService.seedModulesForCommunity(
                    le.getId(),
                    Set.of(ModuleConstants.MODULE_COMMUNITY_FEED,
                            ModuleConstants.MODULE_SPORTS,
                            ModuleConstants.MODULE_EVENTS,
                            ModuleConstants.MODULE_ADMIN_HUB));
            communityLeaderSeeder.seed();

            // Core sports events & tournament
            sportsEventSeeder.seed();
            tournamentSeeder.seed();
            auctionSeeder.seed();
            courtDataSeeder.seed();
            sportsEventRegistrationDataSeeder.seedAllSportsRegistrations();

            // Seed complete tournament lifecycle up to closure
            tournamentLifecycleSeeder.seed();

            log.info("═══════════════════════════════════════════════════════════");
            log.info("  Sports Lifecycle sample data seeded successfully!");
            log.info("═══════════════════════════════════════════════════════════");
        } catch (Exception e) {
            log.error("Failed to seed sports lifecycle data: ", e);
        }
    }

    /**
     * Dedicated method to feed "LE 2026 Season Fest" tournament with all 6 sub-events,
     * base sports metadata, venues, player categories, and auction configurations.
     *
     * @param includeSampleRegistrations if true, seeds sample registrations across Badminton, TT, Chess, Carroms, Volleyball
     */
    @Transactional
    public Map<String, Object> feedSportsData(boolean includeSampleRegistrations) {
        if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            log.warn("Safety guard: feedSportsData cannot run in 'prod' profile.");
            return Map.of("status", "BLOCKED", "message", "Seeding blocked in 'prod' profile.");
        }

        long registrationCount = regRepo != null ? regRepo.count() : 0;
        if (registrationCount > 0) {
            log.warn("SAFETY GUARD TRIGGERED: {} user registrations exist in database. " +
                     "Never wiping data when user registrations exist in database.", registrationCount);
            return Map.of(
                    "status", "BLOCKED",
                    "message", "Operation aborted by safety guard: " + registrationCount + " user registrations exist in database. Cannot wipe or overwrite data.",
                    "timestamp", LocalDateTime.now().toString()
            );
        }

        try {
            log.info("Starting Sports Data Feed (LE 2026 Season Fest)... includeRegistrations={}", includeSampleRegistrations);

            // Layer 1 — users & categories & venues
            userSeeder.seed();
            rolePermissionSeeder.seedUserPermissions();
            playerCategorySeeder.seed();
            sportsMetaSeeder.defaultSeed();
            venueSeeder.seed();

            // Layer 2 — community module activation
            Community le = communitySeeder.getLeCommunity();
            defaultCommunityModuleDataService.seedModulesForCommunity(
                    le.getId(),
                    Set.of(ModuleConstants.MODULE_COMMUNITY_FEED,
                            ModuleConstants.MODULE_SPORTS,
                            ModuleConstants.MODULE_EVENTS,
                            ModuleConstants.MODULE_ADMIN_HUB));
            communityLeaderSeeder.seed();

            // Layer 3 — sports events
            sportsEventSeeder.seed();

            // Layer 4 — tournament linking
            tournamentSeeder.seed();

            // Layer 5 — cricket auction
            auctionSeeder.seed();

            // Layer 6 — courts
            courtDataSeeder.seed();

            int extraRegistrations = 0;
            if (includeSampleRegistrations) {
                extraRegistrations = sportsEventRegistrationDataSeeder.seedAllSportsRegistrations();
            }

            // Layer 7 — complete lifecycle fixtures, standings, playoffs, final, closure
            tournamentLifecycleSeeder.seed();

            log.info("✓ Sports Data Feed completed successfully! (sampleRegistrations={})", includeSampleRegistrations);

            return Map.of(
                    "status", "SUCCESS",
                    "tournament", "LE 2026 Season Fest",
                    "eventDates", "2026-10-02 to 2026-12-31",
                    "registrationDates", "2026-09-17 to 2026-09-19",
                    "totalSubEvents", 24,
                    "subEventsSummary", List.of(
                            "Cricket (3 events): Annual 2026 Cricket Cup (20+ Men Auction), Cricket - Under 10 Kids, Cricket - 11-19 Youth",
                            "Badminton (5 events): Under 12, 12-19 Boys, 12-19 Girls, 19+ Men, 19+ Women",
                            "Chess (5 events): Under 12, 12-19 Boys, 12-19 Girls, 19+ Men, 19+ Women",
                            "Carroms (5 events): Under 12, 12-19 Boys, 12-19 Girls, 19+ Men, 19+ Women",
                            "Table Tennis (5 events): Under 12, 12-19 Boys, 12-19 Girls, 19+ Men, 19+ Women",
                            "Volleyball (1 event): Volleyball Premier League (15+ Men)"
                    ),
                    "sampleRegistrationsIncluded", includeSampleRegistrations,
                    "additionalSampleRegistrationsCount", extraRegistrations,
                    "timestamp", LocalDateTime.now().toString()
            );
        } catch (Exception e) {
            log.error("Failed to feed sports data: ", e);
            return Map.of(
                    "status", "ERROR",
                    "message", "Failed to feed sports data: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
            );
        }
    }

    /**
     * Seeds sample registrations across all sports sub-events.
     */
    @Transactional
    public Map<String, Object> feedSportsRegistrations() {
        if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            log.warn("Safety guard: feedSportsRegistrations cannot run in 'prod' profile.");
            return Map.of("status", "BLOCKED", "message", "Seeding blocked in 'prod' profile.");
        }

        long registrationCount = regRepo != null ? regRepo.count() : 0;
        if (registrationCount > 0) {
            log.warn("SAFETY GUARD TRIGGERED: {} user registrations exist in database. " +
                     "Refusing to overwrite existing user registrations.", registrationCount);
            return Map.of(
                    "status", "BLOCKED",
                    "message", "Operation aborted by safety guard: " + registrationCount + " user registrations exist in database.",
                    "timestamp", LocalDateTime.now().toString()
            );
        }

        try {
            int total = sportsEventRegistrationDataSeeder.seedAllSportsRegistrations();
            return Map.of(
                    "status", "SUCCESS",
                    "message", "Successfully seeded " + total + " sample registrations across sports sub-events.",
                    "totalRegistrationsSeeded", total,
                    "timestamp", LocalDateTime.now().toString()
            );
        } catch (Exception e) {
            log.error("Failed to feed sports registrations: ", e);
            return Map.of(
                    "status", "ERROR",
                    "message", "Failed to feed sports registrations: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
            );
        }
    }
}
