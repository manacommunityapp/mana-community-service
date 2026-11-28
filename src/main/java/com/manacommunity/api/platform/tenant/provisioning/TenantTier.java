package com.manacommunity.api.platform.tenant.provisioning;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Multi-tenant capacity & isolation tiers applied when onboarding a community.
 */
@Getter
@RequiredArgsConstructor
public enum TenantTier {

    /** Standard apartments / gated communities (up to ~500 units). */
    STANDARD(600, 12, 5000L, 5, 25000),

    /** Large residential complexes (500 - 2,000 units). */
    LARGE(1500, 20, 7500L, 10, 50000),

    /** High-density mega-townships (2,000+ units). */
    MEGA_TOWNSHIP(4000, 35, 10000L, 20, 100000);

    private final int rateLimitPerMinute;
    private final int connectionPoolQuota;
    private final long statementTimeoutMs;
    private final int maxConcurrentQueries;
    private final int maxRowsPerExport;

    public static TenantTier fromStringOrDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return STANDARD;
        }
        try {
            return TenantTier.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return STANDARD;
        }
    }
}
