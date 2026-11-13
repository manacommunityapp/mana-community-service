package com.manacommunity.api.sports.model;

/**
 * Determines which completed match stages automatically publish official
 * SPORTS posts into the community feed.
 */
public enum FeedPostPolicy {
    /** Only Grand Finals / Championship matches auto-post to the feed (Recommended default). */
    FINALS_ONLY,

    /** Quarter-Finals, Semi-Finals, 3rd-Place, and Finals auto-post to the feed. */
    KNOCKOUTS_ONLY,

    /** Every completed match generates a feed post (legacy behavior). */
    ALL_MATCHES,

    /** No matches auto-post to the feed (Live strip scoreboard only; manual posting still available). */
    NONE,

    /** Matches matching explicit stage names in `feedPostStages` auto-post to the feed. */
    CUSTOM
}
