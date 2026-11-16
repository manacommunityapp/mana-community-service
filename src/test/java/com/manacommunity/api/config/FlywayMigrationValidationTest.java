package com.manacommunity.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CI Guardrail & Production Correctness Test for Database Migration Discipline.
 *
 * Verifies:
 * 1. Flyway migration file naming and version uniqueness across all migrations.
 * 2. Absence of dangerous destructive statements (e.g. DROP DATABASE).
 * 3. Configuration discipline across application YAML profiles:
 *    - application-prod.yaml MUST use ddl-auto: validate (NEVER update or create).
 *    - application-prod.yaml MUST set clean-disabled: true, out-of-order: false, validate-on-migrate: true.
 *    - application.yaml base config MUST set ddl-auto: validate.
 */
class FlywayMigrationValidationTest {

    private static final Pattern MIGRATION_PATTERN = Pattern.compile("^V([0-9]+(?:_[0-9]+)*)__(.+)\\.sql$");
    private static final Pattern REPEATABLE_PATTERN = Pattern.compile("^R__(.+)\\.sql$");

    @Test
    @DisplayName("Verify all Flyway migration scripts adhere to naming convention and have unique versions")
    void testMigrationNamingAndUniqueness() throws Exception {
        Path migrationDir = Paths.get("src/main/resources/db/migration");
        assertTrue(Files.exists(migrationDir), "db/migration directory must exist");

        List<Path> sqlFiles;
        try (Stream<Path> stream = Files.list(migrationDir)) {
            sqlFiles = stream
                    .filter(p -> p.toString().endsWith(".sql"))
                    .collect(Collectors.toList());
        }

        assertFalse(sqlFiles.isEmpty(), "Expected Flyway migration scripts in db/migration");

        Map<String, Path> versionToPath = new HashMap<>();
        List<String> namingViolations = new ArrayList<>();

        for (Path file : sqlFiles) {
            String filename = file.getFileName().toString();
            Matcher matcher = MIGRATION_PATTERN.matcher(filename);
            if (matcher.matches()) {
                String rawVersion = matcher.group(1);
                // Flyway normalizes underscores to dots for sub-versions: V82_1 -> 82.1
                String normalizedVersion = rawVersion.replace('_', '.');

                if (versionToPath.containsKey(normalizedVersion)) {
                    fail(String.format("Duplicate Flyway version detected: Version '%s' in both '%s' and '%s'",
                            normalizedVersion, versionToPath.get(normalizedVersion).getFileName(), filename));
                }
                versionToPath.put(normalizedVersion, file);
            } else if (!REPEATABLE_PATTERN.matcher(filename).matches()) {
                namingViolations.add(filename);
            }
        }

        assertTrue(namingViolations.isEmpty(),
                "Found migration files violating naming standard (V<version>__<desc>.sql): " + namingViolations);

        // Verify there are over 100 migrations tracked and valid
        assertTrue(versionToPath.size() >= 100,
                "Expected at least 100 migration files, found: " + versionToPath.size());
    }

    @Test
    @DisplayName("Verify migration scripts do not contain unconstrained destructive drop database statements")
    void testNoBannedDestructiveStatements() throws Exception {
        Path migrationDir = Paths.get("src/main/resources/db/migration");
        try (Stream<Path> stream = Files.list(migrationDir)) {
            stream.filter(p -> p.toString().endsWith(".sql")).forEach(file -> {
                try {
                    String content = Files.readString(file).toUpperCase(Locale.ROOT);
                    assertFalse(content.contains("DROP DATABASE"),
                            "Migration script " + file.getFileName() + " contains banned 'DROP DATABASE' statement!");
                } catch (Exception e) {
                    throw new RuntimeException("Failed reading " + file, e);
                }
            });
        }
    }

    @Test
    @DisplayName("Verify production configuration enforces ddl-auto: validate and strict Flyway controls")
    void testProductionConfigurationDiscipline() throws Exception {
        File prodFile = new File("src/main/resources/application-prod.yaml");
        assertTrue(prodFile.exists(), "application-prod.yaml must exist");

        Yaml yaml = new Yaml();
        Map<String, Object> config;
        try (InputStream is = new FileInputStream(prodFile)) {
            config = yaml.load(is);
        }

        assertNotNull(config, "application-prod.yaml could not be parsed");

        Map<String, Object> spring = getSubMap(config, "spring");
        assertNotNull(spring, "spring section missing in application-prod.yaml");

        // Verify JPA Hibernate ddl-auto == validate
        Map<String, Object> jpa = getSubMap(spring, "jpa");
        assertNotNull(jpa, "spring.jpa section missing");
        Map<String, Object> hibernate = getSubMap(jpa, "hibernate");
        assertNotNull(hibernate, "spring.jpa.hibernate section missing");

        Object ddlAuto = hibernate.get("ddl-auto");
        assertEquals("validate", String.valueOf(ddlAuto),
                "CRITICAL: Production MUST use 'ddl-auto: validate' to prohibit automatic schema alterations!");

        // Verify Flyway controls
        Map<String, Object> flyway = getSubMap(spring, "flyway");
        assertNotNull(flyway, "spring.flyway section missing in application-prod.yaml");

        assertEquals(Boolean.TRUE, flyway.get("enabled"),
                "spring.flyway.enabled must be true in production");
        assertEquals(Boolean.TRUE, flyway.get("clean-disabled"),
                "spring.flyway.clean-disabled must be true in production");
        assertEquals(Boolean.FALSE, flyway.get("out-of-order"),
                "spring.flyway.out-of-order must be false in production to ensure sequential execution");
        assertEquals(Boolean.TRUE, flyway.get("validate-on-migrate"),
                "spring.flyway.validate-on-migrate must be true in production");
    }

    @Test
    @DisplayName("Verify base application.yaml sets ddl-auto: validate and strict Flyway properties")
    void testBaseApplicationConfigurationDiscipline() throws Exception {
        File baseFile = new File("src/main/resources/application.yaml");
        assertTrue(baseFile.exists(), "application.yaml must exist");

        Yaml yaml = new Yaml();
        Map<String, Object> config;
        try (InputStream is = new FileInputStream(baseFile)) {
            config = yaml.load(is);
        }

        assertNotNull(config, "application.yaml could not be parsed");
        Map<String, Object> spring = getSubMap(config, "spring");
        assertNotNull(spring, "spring section missing in application.yaml");

        // Base JPA ddl-auto must be validate
        Map<String, Object> jpa = getSubMap(spring, "jpa");
        assertNotNull(jpa, "spring.jpa missing");
        Map<String, Object> hibernate = getSubMap(jpa, "hibernate");
        assertNotNull(hibernate, "spring.jpa.hibernate missing");
        assertEquals("validate", String.valueOf(hibernate.get("ddl-auto")),
                "Base application.yaml must default ddl-auto to 'validate'");

        // Base Flyway controls
        Map<String, Object> flyway = getSubMap(spring, "flyway");
        assertNotNull(flyway, "spring.flyway missing in base application.yaml");
        assertEquals(Boolean.TRUE, flyway.get("clean-disabled"),
                "Base spring.flyway.clean-disabled must be true");
        assertEquals(Boolean.FALSE, flyway.get("out-of-order"),
                "Base spring.flyway.out-of-order must be false");
        assertEquals(Boolean.TRUE, flyway.get("validate-on-migrate"),
                "Base spring.flyway.validate-on-migrate must be true");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSubMap(Map<String, Object> parent, String key) {
        Object val = parent.get(key);
        if (val instanceof Map) {
            return (Map<String, Object>) val;
        }
        return null;
    }
}
