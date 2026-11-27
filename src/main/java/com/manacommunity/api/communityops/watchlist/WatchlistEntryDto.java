package com.manacommunity.api.communityops.watchlist;

import java.time.Instant;

public record WatchlistEntryDto(
    WatchlistType type,
    String name,
    String identifier,
    String reason,
    WatchlistSeverity severity,
    Instant expiresAt
) {}
