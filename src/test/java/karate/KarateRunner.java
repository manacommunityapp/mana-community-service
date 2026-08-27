package karate;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.Tag;

/**
 * Karate JUnit 5 runner — three independent module flows.
 *
 * ┌──────────────────────────────────────────────────────────┐
 * │  Module         │  Runner method   │  Tag              │
 * ├──────────────────────────────────────────────────────────┤
 * │  Signup / Auth  │  signupFlow()    │  @signup          │
 * │  Community Feed │  feedFlow()      │  @feed            │
 * │  Event Module   │  poojaFlow()     │  @pooja @e2e      │
 * │  DB Verify      │  dbVerify()      │  @db-verify       │
 * │  Full smoke     │  smoke()         │  @smoke           │
 * │  All tests      │  all()           │  ~@ignore         │
 * └──────────────────────────────────────────────────────────┘
 *
 * Run commands:
 *   All tests           : ./mvnw test -Dtest=KarateRunner
 *   Signup flow only    : ./mvnw test -Dtest=KarateRunner#signupFlow
 *   Feed flow only      : ./mvnw test -Dtest=KarateRunner#feedFlow
 *   Event / Pooja flow  : ./mvnw test -Dtest=KarateRunner#poojaFlow
 *   DB assertions only  : ./mvnw test -Dtest=KarateRunner#dbVerify
 *   Quick smoke (all)   : ./mvnw test -Dtest=KarateRunner#smoke
 *   Staging env         : ./mvnw test -Dtest=KarateRunner#smoke -Dkarate.env=staging
 */
@Tag("karate")
class KarateRunner {

    /** Full suite — skips anything tagged @ignore */
    @Karate.Test
    Karate all() {
        return Karate.run("classpath:karate/features")
                     .tags("~@ignore");
    }

    /** Quick gate — @smoke scenarios across all modules */
    @Karate.Test
    Karate smoke() {
        return Karate.run("classpath:karate/features")
                     .tags("@smoke");
    }

    // ── Module flows ──────────────────────────────────────────────────────────

    /**
     * Signup & Auth flow: registration, login, profile, token refresh, password change, logout.
     * Feature directory: features/signup/
     */
    @Karate.Test
    Karate signupFlow() {
        return Karate.run("classpath:karate/features/signup")
                     .tags("~@ignore");
    }

    /**
     * Community Feed flow: CRUD posts, comments, likes, reactions, bookmarks, search.
     * Feature directory: features/feed/
     */
    @Karate.Test
    Karate feedFlow() {
        return Karate.run("classpath:karate/features/feed")
                     .tags("~@ignore");
    }

    /**
     * Event module flow: Ganesh Mahotsav E2E — 250 users, event create,
     * pooja seva setup, devotee registration, admin registration.
     * Feature directory: features/e2e/, features/pooja/, features/events/
     */
    @Karate.Test
    Karate poojaFlow() {
        return Karate.run("classpath:karate/features/e2e")
                     .tags("~@ignore");
    }

    /**
     * Database-state verification only — run after poojaFlow completes.
     * Feature directory: features/db/
     */
    @Karate.Test
    Karate dbVerify() {
        return Karate.run("classpath:karate/features/db")
                     .tags("@db-verify");
    }
}
