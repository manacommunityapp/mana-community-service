package com.manacommunity.api.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource, Environment env) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas("manacommunity")
                .locations("classpath:db/migration", "classpath:db/seed")
                .baselineOnMigrate(true)
                .baselineVersion("115")
                .baselineDescription("pre-flyway baseline schema")
                .outOfOrder(true)
                .load();

        try {
            flyway.repair();
        } catch (Exception e) {
            log.warn("Flyway repair warning: {}", e.getMessage());
        }

        // Only reset stale schema history on local profile — dev/prod track migrations properly
        if (Arrays.asList(env.getActiveProfiles()).contains("local")) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                    "SELECT MAX(CAST(version AS INTEGER)) FROM manacommunity.flyway_schema_history WHERE success = true");
                if (rs.next()) {
                    int maxVersion = rs.getInt(1);
                    if (maxVersion < 115) {
                        log.info("Flyway schema history is at V{}, resetting to baseline at V115", maxVersion);
                        stmt.execute("DROP TABLE manacommunity.flyway_schema_history");
                    }
                }
            } catch (Exception e) {
                log.info("Flyway schema history check: {}", e.getMessage());
            }
        }

        return flyway;
    }
}
