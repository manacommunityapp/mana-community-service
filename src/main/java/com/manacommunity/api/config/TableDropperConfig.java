package com.manacommunity.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Premium DDL Utility: Startup Table Dropper Config
 *
 * Allows safely dropping one or more specific tables on startup BEFORE Hibernate
 * initializes its EntityManagerFactory and runs 'ddl-auto: update'.
 *
 * Supports a comma-separated list of table names via:
 *   app.drop-table-on-startup.table-names=tournament_config,tournament_match,tournament_group
 *
 * Tables are dropped in the order listed, each with CASCADE, so FK dependencies
 * are handled automatically by PostgreSQL.
 */
@Configuration
@org.springframework.context.annotation.Profile("local")
public class TableDropperConfig {

    private static final Logger log = LoggerFactory.getLogger(TableDropperConfig.class);

    /**
     * Standard Spring BeanFactoryPostProcessor to force any EntityManagerFactory bean definitions
     * to depend on our 'tableDropper' bean.
     *
     * This guarantees that TableDropper executes its DDL statement BEFORE Hibernate scans
     * database schemas or creates/updates schemas.
     */
    @Component
    public static class EntityManagerDependsOnTableDropperPostProcessor implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            log.info("BeanFactoryPostProcessor mapping EntityManagerFactory to depend on tableDropper...");
            try {
                String[] beanNames = beanFactory.getBeanNamesForType(jakarta.persistence.EntityManagerFactory.class, true, false);
                for (String beanName : beanNames) {
                    BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
                    String[] dependsOn = definition.getDependsOn();
                    List<String> newDependsOn = dependsOn == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(dependsOn));
                    if (!newDependsOn.contains("tableDropper")) {
                        newDependsOn.add("tableDropper");
                        definition.setDependsOn(newDependsOn.toArray(new String[0]));
                        log.info("Successfully added 'tableDropper' dependency to bean '{}'", beanName);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to register TableDropper bean dependencies: " + e.getMessage(), e);
            }
        }
    }

    @Component("tableDropper")
    public static class TableDropper {

        private static final Logger log = LoggerFactory.getLogger(TableDropper.class);

        @Autowired
        private DataSource dataSource;

        @Value("${app.drop-table-on-startup.enabled:false}")
        private boolean enabled;

        /**
         * Comma-separated list of table names to drop.
         * Example: tournament_config,tournament_match,tournament_group,group_team_standing
         */
        @Value("${app.drop-table-on-startup.table-names:}")
        private String tableNames;

        @PostConstruct
        public void dropTablesIfExists() {
            if (!enabled) {
                log.info("Startup TableDropper is disabled (app.drop-table-on-startup.enabled = false). Skipping.");
                return;
            }

            if (tableNames == null || tableNames.trim().isEmpty()) {
                log.warn("Startup TableDropper is enabled, but no table names were provided. Skipping.");
                return;
            }

            String[] tables = tableNames.split(",");
            log.info("Startup TableDropper active: Will attempt to drop {} table(s)...", tables.length);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                for (String rawName : tables) {
                    String sanitized = rawName.trim().replaceAll("[^a-zA-Z0-9_.]", "");
                    if (sanitized.isEmpty()) continue;

                    String sql = "DROP TABLE IF EXISTS " + sanitized + " CASCADE";
                    log.info("Executing: {}", sql);
                    stmt.execute(sql);
                    log.info("✓ Table '{}' dropped successfully.", sanitized);
                }

                log.info("Startup TableDropper finished. All requested tables processed.");

            } catch (Exception e) {
                log.error("Startup TableDropper encountered an error: " + e.getMessage(), e);
            }
        }
    }
}
