package com.manacommunity.api.sports.model;

public enum MatchStatus {
    DRAFT,          // saved as draft — visible only to the organiser
    PUBLISHED,      // published — visible to all participants
    SCHEDULED,
    CONFIRMED,      // teams and officials confirmed
    CHECK_IN_OPEN,  // participant check-in open
    IN_PROGRESS,    // match underway (alias for backward compat with LIVE)
    LIVE,
    SUSPENDED,      // temporarily interrupted (rain, power, etc.)
    RESUMED,        // match restarted after suspension
    COMPLETED,
    POSTPONED,      // moved to another date before match started
    RESCHEDULED,    // match date/time updated and confirmed
    ABANDONED,      // match stopped without completion after start
    NO_RESULT,      // no valid result possible
    CANCELLED,
    FORFEITED,      // team forfeited
    WALKOVER,       // opponent awarded victory
    UNDER_REVIEW,   // result or incident under review
    BYE,            // auto-advance when team count is not a power of 2
    AUTO_ADVANCED   // BYE match whose seeded player was auto-progressed to the next round
}
