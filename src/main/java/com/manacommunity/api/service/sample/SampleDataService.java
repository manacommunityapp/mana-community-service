package com.manacommunity.api.service.sample;

import com.manacommunity.api.service.sample.data.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PlayerCategorySeeder playerCategorySeeder;
    private final VenueSeeder venueSeeder;
    private final CommunityLeaderSeeder communityLeaderSeeder;
    private final SportsEventSeeder sportsEventSeeder;
    private final TournamentSeeder tournamentSeeder;
    private final AuctionSeeder auctionSeeder;
    private final InventorySeeder inventorySeeder;

    private final DefaultCommunityModuleDataService defaultCommunityModuleDataService;
    // Dedicated per-table sample seeders
    private final RolePermissionDataSeeder rolePermissionDataSeeder;
    private final VenueDataSeeder venueDataSeeder;
    private final CourtDataSeeder courtDataSeeder;
    private final SportsEventDataSeeder sportsEventDataSeeder;
    private final SportsEventRegistrationDataSeeder sportsEventRegistrationDataSeeder;
    private final TournamentDataSeeder tournamentDataSeeder;
    private final EmailTemplateFeeder emailTemplateFeeder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
//        try {
//            entityManager.createNativeQuery("ALTER TABLE manacommunity.role_permissions DROP CONSTRAINT IF EXISTS ukan4n77iv8oyxb9vm5ce46nly").executeUpdate();
//            log.info("✓ Legacy unique constraint ukan4n77iv8oyxb9vm5ce46nly dropped successfully on startup.");
//        } catch (Exception e) {
//            log.warn("Could not drop legacy unique constraint on startup: {}", e.getMessage());
//        }

        try {
            entityManager.createNativeQuery("SELECT setval('manacommunity.roles_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM manacommunity.roles), false)").getSingleResult();
            log.info("✓ Roles sequence synchronized to next available value successfully.");
        } catch (Exception e) {
            log.warn("Could not synchronize roles sequence on startup: {}", e.getMessage());
        }

        boolean insertSampleData = environment.getProperty("app.insert-sample-data", Boolean.class, false);
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto", "none");
        if ("update".equalsIgnoreCase(ddlAuto)) {
            log.info("DDL-Auto is 'update'.");
            if (insertSampleData) {
                log.info("Running sample data seeding as app.insert-sample-data is enabled...");
                executeSampleDataSql();
            }
        } else if ("create".equalsIgnoreCase(ddlAuto)) {
            log.info("DDL-Auto is 'create'. Running default data seeding...");
            executeDefaultDataSql();
            if (insertSampleData) {
                log.info("Running sample data seeding as app.insert-sample-data is enabled...");
                executeSampleDataSql();
            }
        } else {
            log.info("DDL-Auto is '{}'.", ddlAuto);
            if (insertSampleData) {
                log.info("Running sample data seeding as app.insert-sample-data is enabled...");
                executeSampleDataSql();
            }
        }
    }

    @Transactional
    public String executeDefaultDataSql() {
        try{
            communitySeeder.defaultSeed();
            rolePermissionSeeder.defaultSeed();
            userSeeder.defaultSeed();

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
//            try {
//                entityManager.createNativeQuery("ALTER TABLE manacommunity.role_permissions DROP CONSTRAINT IF EXISTS ukan4n77iv8oyxb9vm5ce46nly").executeUpdate();
//                log.info("✓ Legacy unique constraint ukan4n77iv8oyxb9vm5ce46nly dropped successfully.");
//            } catch (Exception e) {
//                log.warn("Could not drop legacy unique constraint: {}", e.getMessage());
//            }

            //executeDefaultDataSql();
            // ════════════════════════════════════════════════════════════════════
            // Execute modular seeders in strict dependency order
            // ════════════════════════════════════════════════════════════════════
            //rolePermissionSeeder.seed();
            //communitySeeder.seed();
            userSeeder.seed();
            rolePermissionSeeder.seedUserPermissions();
            playerCategorySeeder.seed();
            venueSeeder.seed();
            //inventorySeeder.seed();
            defaultCommunityModuleDataService.seedDefaultModulesForCommunity(2L);
            communityLeaderSeeder.seed();
            //emailTemplateFeeder.seed();
            //sportsEventSeeder.seed();
            //tournamentSeeder.seed();
            //auctionSeeder.seed();

            // Dedicated per-table seeders (run after their dependencies above)
            //rolePermissionDataSeeder.seed();
            //venueDataSeeder.seed();
            //courtDataSeeder.seed();
            //sportsEventDataSeeder.seed();
            //sportsEventRegistrationDataSeeder.seed();
            //tournamentDataSeeder.seed();

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
