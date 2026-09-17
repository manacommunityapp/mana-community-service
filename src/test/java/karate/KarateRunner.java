package karate;

import com.intuit.karate.Runner;
import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

/**
 * Karate JUnit 5 runner — module flows for the Mana Community service.
 *
 * ┌─────────────────────────────┬──────────────────────────────────────────────────────────┐
 * │  Method                     │  What it runs                                            │
 * ├─────────────────────────────┼──────────────────────────────────────────────────────────┤
 * │  all()                      │  Every non-@ignore feature                               │
 * │  smoke()                    │  @smoke scenarios across all modules                     │
 * │  signupFlow()               │  Auth / signup module                                    │
 * │  feedFlow()                 │  Community feed module                                   │
 * │  poojaFlow()                │  Full event module (events + pooja + e2e)                │
 * │  eventListFlow()            │  List / fetch events (standalone)                        │
 * │  eventDashboardFlow()       │  Dashboard stats / analytics / pending-actions           │
 * │  poojaTypesFlow()           │  Pooja types CRUD                                        │
 * │  dbVerify()                 │  Event DB state assertions (@db-verify)                  │
 * ├─────────────────────────────┼──────────────────────────────────────────────────────────┤
 * │  sportsMetaFlow()           │  Sport meta CRUD (cricket/badminton types)               │
 * │  sportsCategoriesFlow()     │  Player categories CRUD                                  │
 * │  sportsDashboardFlow()      │  Sports dashboard stats endpoints                        │
 * │  sportsScheduleFlow()       │  Sports schedule stats + open/mine endpoints             │
 * │  sportsRankingsFlow()       │  Player rankings GET + upsert                            │
 * │  sportsEventsFlow()         │  Sports events listing (all, open, mine, admin views)    │
 * │  sportsTournamentFlow()     │  Tournament CRUD + content + scheduler                   │
 * │  sportsAuctionFlow()        │  Auction config + teams + live bidding flow              │
 * │  sportsE2eFlow()            │  Full sports E2E (cricket tournament lifecycle)          │
 * │  sportsDbVerify()           │  Sports DB state assertions (@sports-db-verify)          │
 * └─────────────────────────────┴──────────────────────────────────────────────────────────┘
 *
 * Run commands (PowerShell — quote the method selector to avoid # truncation):
 *   All tests                : ./mvnw test -Dtest=KarateRunner
 *   Full sports E2E          : ./mvnw test "-Dtest=KarateRunner#sportsE2eFlow"
 *   Sports meta              : ./mvnw test "-Dtest=KarateRunner#sportsMetaFlow"
 *   Sports categories        : ./mvnw test "-Dtest=KarateRunner#sportsCategoriesFlow"
 *   Sports dashboard         : ./mvnw test "-Dtest=KarateRunner#sportsDashboardFlow"
 *   Sports schedule          : ./mvnw test "-Dtest=KarateRunner#sportsScheduleFlow"
 *   Sports rankings          : ./mvnw test "-Dtest=KarateRunner#sportsRankingsFlow"
 *   Sports events list       : ./mvnw test "-Dtest=KarateRunner#sportsEventsFlow"
 *   Sports tournament        : ./mvnw test "-Dtest=KarateRunner#sportsTournamentFlow"
 *   Sports auction           : ./mvnw test "-Dtest=KarateRunner#sportsAuctionFlow"
 *   Sports DB verify         : ./mvnw test "-Dtest=KarateRunner#sportsDbVerify"
 *   Full event module        : ./mvnw test "-Dtest=KarateRunner#poojaFlow"
 *   Event list check         : ./mvnw test "-Dtest=KarateRunner#eventListFlow"
 *   Dashboard check          : ./mvnw test "-Dtest=KarateRunner#eventDashboardFlow"
 *   Pooja types check        : ./mvnw test "-Dtest=KarateRunner#poojaTypesFlow"
 *   Signup flow              : ./mvnw test "-Dtest=KarateRunner#signupFlow"
 *   Feed flow                : ./mvnw test "-Dtest=KarateRunner#feedFlow"
 *   DB assertions only       : ./mvnw test "-Dtest=KarateRunner#dbVerify"
 *   Quick smoke (all)        : ./mvnw test "-Dtest=KarateRunner#smoke"
 *   Staging env              : ./mvnw test "-Dtest=KarateRunner#smoke" -Dkarate.env=staging
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

    // ── Sports module flows ───────────────────────────────────────────────────

    /**
     * Sport meta CRUD — standalone, admin auth only.
     * Covers: GET/POST/PUT/DELETE /api/sports/meta
     */
    @Karate.Test
    Karate sportsMetaFlow() {
        return Karate.run("classpath:karate/features/sports/meta/sports-meta.feature")
                     .tags("~@ignore");
    }

    /**
     * Player categories CRUD — standalone, admin auth only.
     * Covers: GET/POST/PUT/DELETE /api/player-categories
     */
    @Karate.Test
    Karate sportsCategoriesFlow() {
        return Karate.run("classpath:karate/features/sports/categories/player-categories.feature")
                     .tags("~@ignore");
    }

    /**
     * Sports dashboard stats — standalone, admin auth only.
     * Covers: all 6 GET /sports/dashboard/* endpoints
     */
    @Karate.Test
    Karate sportsDashboardFlow() {
        return Karate.run("classpath:karate/features/sports/dashboard/sports-dashboard.feature")
                     .tags("~@ignore");
    }

    /**
     * Sports schedule / stats — standalone, admin auth only.
     * Covers: 5 stats endpoints + open events + mine registrations
     */
    @Karate.Test
    Karate sportsScheduleFlow() {
        return Karate.run("classpath:karate/features/sports/schedule/schedule-stats.feature")
                     .tags("~@ignore");
    }

    /**
     * Player rankings — standalone, admin auth + user search.
     * Covers: GET /sports/rankings + upsert via admin user-search
     */
    @Karate.Test
    Karate sportsRankingsFlow() {
        return Karate.run("classpath:karate/features/sports/rankings/player-rankings.feature")
                     .tags("~@ignore");
    }

    /**
     * Sports events listing — standalone, admin auth only.
     * Covers: /sports/events/all, /open, /open-all, /closed, /mine, /community,
     *         /admin/overview, /admin/form-data, single event fetch
     */
    @Karate.Test
    Karate sportsEventsFlow() {
        return Karate.run("classpath:karate/features/sports/events/list-sports-events.feature")
                     .tags("~@ignore");
    }

    /**
     * Sports tournament flow — requires sports users and event created first.
     * Covers: create tournament → announcements → gallery → timeline → scheduler config → generate schedule
     */
    @Karate.Test
    Karate sportsTournamentFlow() {
        return Karate.run(
                "classpath:karate/features/sports/tournament"
        ).tags("~@ignore");
    }

    /**
     * Sports auction flow — requires auction config context from E2E (or run after sportsE2eFlow).
     * Covers: auction config DRAFT→ACTIVE → teams → players → LIVE bidding → COMPLETED
     */
    @Karate.Test
    Karate sportsAuctionFlow() {
        return Karate.run(
                "classpath:karate/features/sports/auction"
        ).tags("~@ignore");
    }

    /**
     * Full sports E2E — orchestrates all sports sub-flows in sequence.
     * Steps: users → meta → categories → cricket event → register → status update →
     *        partner invite (badminton) → tournament → content → scheduler → auction → DB verify
     */
    @Karate.Test
    Karate sportsE2eFlow() {
        return Karate.run(
                "classpath:karate/features/users/create-sports-users.feature",
                "classpath:karate/features/sports",
                "classpath:karate/features/sports/e2e/cricket-tournament-e2e.feature"
        ).tags("@e2e", "@sports", "~@ignore");
    }

    /**
     * Sports database-state verification only.
     * Run after sportsE2eFlow to assert DB rows were created correctly.
     */
    @Karate.Test
    Karate sportsDbVerify() {
        return Karate.run("classpath:karate/features/sports/db/verify-sports-db.feature")
                     .tags("@sports-db-verify");
    }
}
