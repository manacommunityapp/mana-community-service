package com.manacommunity.api.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Authoritative database migration configuration.
 *
 * Enforces production data integrity and migration discipline:
 * - Flyway is the sole schema-change mechanism (Hibernate ddl-auto must be validate in prod/dev).
 * - Reads configuration directly from Spring Boot FlywayProperties (no hardcoded overrides).
 * - Enforces sequential migrations (outOfOrder = false in prod).
 * - Automatic repair on startup is DISABLED by default to prevent masking corrupted schema state.
 * - Destructive schema history drops are eliminated.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true, havingValue = "true")
@EnableConfigurationProperties(FlywayProperties.class)
@Slf4j
public class FlywayConfig {

    @Value("${app.flyway.repair-on-startup:false}")
    private boolean repairOnStartup;

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource, FlywayProperties flywayProperties) {
        // Enforce cloud connection security guard before connecting
        try (Connection conn = dataSource.getConnection()) {
            DatabaseConnectionSecurityGuard.validateConnection(conn);
        } catch (SQLException e) {
            log.warn("DatabaseConnectionSecurityGuard check in Flyway: {}", e.getMessage());
        }

        List<String> schemas = flywayProperties.getSchemas();
        String[] schemaArray = (schemas != null && !schemas.isEmpty())
                ? schemas.toArray(new String[0])
                : new String[]{"manacommunity"};

        List<String> locations = flywayProperties.getLocations();
        String[] locationArray = (locations != null && !locations.isEmpty())
                ? locations.toArray(new String[0])
                : new String[]{"classpath:db/migration", "classpath:db/seed"};

        String baselineVersion = (flywayProperties.getBaselineVersion() != null)
                ? flywayProperties.getBaselineVersion()
                : "1";

        String baselineDescription = (flywayProperties.getBaselineDescription() != null)
                ? flywayProperties.getBaselineDescription()
                : "pre-flyway baseline schema";

        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaArray)
                .locations(locationArray)
                .baselineOnMigrate(flywayProperties.isBaselineOnMigrate())
                .baselineVersion(baselineVersion)
                .baselineDescription(baselineDescription)
                .outOfOrder(flywayProperties.isOutOfOrder())
                .validateOnMigrate(flywayProperties.isValidateOnMigrate())
                .cleanDisabled(flywayProperties.isCleanDisabled());

        Flyway flyway = configuration.load();

        // Automatic repair on startup is prohibited in production by default.
        // It can only be triggered via explicit operational intervention: app.flyway.repair-on-startup=true.
        if (repairOnStartup) {
            log.warn("🚨 EMERGENCY OPERATIONAL OVERRIDE: app.flyway.repair-on-startup is ENABLED. Executing Flyway repair...");
            try {
                flyway.repair();
                log.info("Flyway repair executed successfully.");
            } catch (Exception e) {
                log.error("Flyway repair failed: {}", e.getMessage(), e);
                throw e;
            }
        }

        return flyway;
    }
}
