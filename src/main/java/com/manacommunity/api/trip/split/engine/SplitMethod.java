package com.manacommunity.api.trip.split.engine;

/** How an expense is divided among its participants. */
public enum SplitMethod {
    /** Equal parts among the chosen participants (covers per-person and exclude). */
    EQUAL,
    /** Percentage per participant; must total 100%. */
    PERCENTAGE,
    /** Exact amount per participant; must total the expense amount (covers custom split). */
    EXACT,
    /** Share proportional to units consumed per participant (e.g. rooms used). */
    QUANTITY,
    /** Weighted shares, e.g. a family of four represented by one participant with weight 4. */
    SHARES
}
