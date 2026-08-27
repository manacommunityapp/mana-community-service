package karate;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.Tag;

/**
 * Karate JUnit 5 runner.
 *
 * Run all:     ./mvnw test -Dtest=KarateRunner
 * Smoke only:  ./mvnw test -Dtest=KarateRunner#smoke
 * Pooja flow:  ./mvnw test -Dtest=KarateRunner#poojaFlow
 * DB verify:   ./mvnw test -Dtest=KarateRunner#dbVerify
 *
 * Switch environment: ./mvnw test -Dtest=KarateRunner -Dkarate.env=staging
 */
@Tag("karate")
class KarateRunner {

    /** Full suite — skips anything tagged @ignore */
    @Karate.Test
    Karate all() {
        return Karate.run("classpath:karate/features")
                     .tags("~@ignore");
    }

    /** Fast smoke check — tags @smoke only */
    @Karate.Test
    Karate smoke() {
        return Karate.run("classpath:karate/features")
                     .tags("@smoke");
    }

    /** End-to-end Ganesh Mahotsav flow */
    @Karate.Test
    Karate poojaFlow() {
        return Karate.run("classpath:karate/features/e2e")
                     .tags("@pooja");
    }

    /** Database-state verification only */
    @Karate.Test
    Karate dbVerify() {
        return Karate.run("classpath:karate/features/db")
                     .tags("@db-verify");
    }
}
