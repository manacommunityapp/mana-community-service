package karate;

import com.intuit.karate.Runner;
import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

/**
 * Karate JUnit 5 runner — module flows for the Mana Community service.
 *
 * ┌────────────────────────────────────────────────────────────────────────────┐
 * │  Method               │  What it runs                                     │
 * ├────────────────────────────────────────────────────────────────────────────┤
 * │  all()                │  Every non-@ignore feature                        │
 * │  smoke()              │  @smoke tagged scenarios across all modules        │
 * │  signupFlow()         │  Auth / signup module                             │
 * │  feedFlow()           │  Community feed module                            │
 * │  poojaFlow()          │  Full event module (events + pooja + e2e)         │
 * │  eventListFlow()      │  List / fetch events (standalone)                 │
 * │  eventDashboardFlow() │  Dashboard stats / analytics / pending-actions    │
 * │  poojaTypesFlow()     │  Pooja types CRUD                                 │
 * │  dbVerify()           │  DB state assertions (@db-verify)                 │
 * └────────────────────────────────────────────────────────────────────────────┘
 *
 * Run commands (PowerShell — quote the method selector to avoid # truncation):
 *   All tests              : ./mvnw test -Dtest=KarateRunner
 *   Full event module      : ./mvnw test "-Dtest=KarateRunner#poojaFlow"
 *   Event list check       : ./mvnw test "-Dtest=KarateRunner#eventListFlow"
 *   Dashboard check        : ./mvnw test "-Dtest=KarateRunner#eventDashboardFlow"
 *   Pooja types check      : ./mvnw test "-Dtest=KarateRunner#poojaTypesFlow"
 *   Signup flow            : ./mvnw test "-Dtest=KarateRunner#signupFlow"
 *   Feed flow              : ./mvnw test "-Dtest=KarateRunner#feedFlow"
 *   DB assertions only     : ./mvnw test "-Dtest=KarateRunner#dbVerify"
 *   Quick smoke (all)      : ./mvnw test "-Dtest=KarateRunner#smoke"
 *   Staging env            : ./mvnw test "-Dtest=KarateRunner#smoke" -Dkarate.env=staging
 */
@Tag("karate")
class KarateRunner {

    /** Wipe test data before any flow starts — prevents stale rows blocking re-runs. */
    @BeforeAll
    static void cleanBefore() {
        Runner.path("classpath:karate/features/db/cleanup.feature")
              .tags("@cleanup")
              .parallel(1);
    }

    /** Wipe test data after all flows finish — leaves the DB in a clean state. */
    @AfterAll
    static void cleanAfter() {
        Runner.path("classpath:karate/features/db/cleanup.feature")
              .tags("@cleanup")
              .parallel(1);
    }

    // ── Full suites ───────────────────────────────────────────────────────────

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
     */
    @Karate.Test
    Karate signupFlow() {
        return Karate.run("classpath:karate/features/signup")
                     .tags("~@ignore");
    }

    /**
     * Community Feed flow: CRUD posts, comments, likes, reactions, bookmarks, search.
     */
    @Karate.Test
    Karate feedFlow() {
        return Karate.run("classpath:karate/features/feed")
                     .tags("~@ignore");
    }

    /**
     * Full event module flow:
     *   features/events/ — list, dashboard, pooja-types, event-registration, update-event
     *   features/pooja/  — create-sevas, register, admin-register, schedule-mgmt, reschedule
     *   features/e2e/    — orchestrated full lifecycle (250 users → event → seva → register → DB verify)
     */
    @Karate.Test
    Karate poojaFlow() {
        return Karate.run(
                "classpath:karate/features/events",
                "classpath:karate/features/pooja",
                "classpath:karate/features/e2e"
        ).tags("~@ignore");
    }

    /**
     * Event list / fetch flow — standalone, no test data setup needed.
     * Covers: GET /events, GET /events/all, GET /events?type=, GET /events/mine, GET /events/{id}
     */
    @Karate.Test
    Karate eventListFlow() {
        return Karate.run("classpath:karate/features/events/list-events.feature")
                     .tags("~@ignore");
    }

    /**
     * Event dashboard — standalone, admin auth only.
     * Covers: GET /events/dashboard/stats, /analytics, /pending-actions
     */
    @Karate.Test
    Karate eventDashboardFlow() {
        return Karate.run("classpath:karate/features/events/event-dashboard.feature")
                     .tags("~@ignore");
    }

    /**
     * Pooja types CRUD — standalone, admin auth only.
     * Covers: GET /events/pooja-types, POST /events/pooja-types
     */
    @Karate.Test
    Karate poojaTypesFlow() {
        return Karate.run("classpath:karate/features/events/pooja-types.feature")
                     .tags("~@ignore");
    }

    /**
     * Database-state verification only — run after poojaFlow completes.
     */
    @Karate.Test
    Karate dbVerify() {
        return Karate.run("classpath:karate/features/db")
                     .tags("@db-verify");
    }
}
