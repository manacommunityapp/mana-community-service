package com.manacommunity.api.service.sample;

import com.manacommunity.api.constants.ModuleConstants;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.service.sample.data.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * SampleDataService — Seeds the database with baseline data matching
 * the current production state by orchestrating dedicated seeder services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleDataService implements ApplicationRunner {

    private final jakarta.persistence.EntityManager entityManager;
    private final Environment environment;

    private final RolePermissionSeeder rolePermissionSeeder;
    private final CommunitySeeder communitySeeder;
    private final SportsMetaSeeder sportsMetaSeeder;
    private final UserSeeder userSeeder;
    private final SportsPlayerCategorySeeder playerCategorySeeder;
    private final VenueSeeder venueSeeder;
    private final CommunityLeaderSeeder communityLeaderSeeder;
    private final SportsEventSeeder sportsEventSeeder;
    private final SportsTournamentSeeder tournamentSeeder;
    private final SportsAuctionSeeder auctionSeeder;
    private final InventorySeeder inventorySeeder;

    private final DefaultCommunityModuleDataService defaultCommunityModuleDataService;
    // Dedicated per-table sample seeders
    private final RolePermissionDataSeeder rolePermissionDataSeeder;
    private final VenueDataSeeder venueDataSeeder;
    private final SportsCourtDataSeeder courtDataSeeder;
    private final SportsEventDataSeeder sportsEventDataSeeder;
    private final SportsEventRegistrationDataSeeder sportsEventRegistrationDataSeeder;
    private final SportsTournamentDataSeeder tournamentDataSeeder;
    private final EmailTemplateFeeder emailTemplateFeeder;
    private final com.manacommunity.api.user.repository.AppUserRepository userRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            entityManager.createNativeQuery("SELECT setval('manacommunity.roles_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM manacommunity.roles), false)").getSingleResult();
            log.info("✓ Roles sequence synchronized to next available value successfully.");
        } catch (Exception e) {
            log.warn("Could not synchronize roles sequence on startup: {}", e.getMessage());
        }

        long userCount = userRepo.count();
        log.info("Current AppUser count in database: {}", userCount);
        if (userCount == 0) {
            log.info("AppUser table is empty! Executing default user & community seeding...");
            executeDefaultDataSql();
            userSeeder.seed();
        }

        boolean insertSampleData = environment.getProperty("app.insert-sample-data", Boolean.class, false);
        if (insertSampleData) {
            log.info("Running sample data seeding as app.insert-sample-data is enabled...");
            executeSampleDataSql();
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
            tournamentDataSeeder.seed();

            // Layer 5 — auction (depends on sports events and users)
            auctionSeeder.seed();

            // Layer 6 — dedicated per-table seeders (depend on previous layers)
            courtDataSeeder.seed();
            sportsEventDataSeeder.seed();
            sportsEventRegistrationDataSeeder.seed();

            log.info("═══════════════════════════════════════════════════════════");
            log.info("  Java-based database seeding completed successfully!");
            log.info("═══════════════════════════════════════════════════════════");
            return "Sample data successfully seeded using Java repositories!";

        } catch (Exception e) {
            log.error("Failed to seed database: ", e);
            return "Failed to execute sample data: " + e.getMessage();
        }
    }
}
