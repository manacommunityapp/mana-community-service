package com.manacommunity.api.platform.tenant;

import com.manacommunity.api.exception.ManaCommunityException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a single tenant attempts to borrow more concurrent database connections
 * than its allocated bulkhead quota allows, preventing noisy-neighbor pool starvation (LDT-002).
 */
public class TenantConnectionQuotaExceededException extends ManaCommunityException {

    private final Long communityId;
    private final int activeLeases;
    private final int maxAllowed;

    public TenantConnectionQuotaExceededException(Long communityId, int activeLeases, int maxAllowed) {
        super(
                String.format("Tenant connection quota exceeded for community %d: active=%d, maxAllowed=%d. Fair-share bulkhead protected.",
                        communityId, activeLeases, maxAllowed),
                HttpStatus.TOO_MANY_REQUESTS,
                "TENANT_CONNECTION_QUOTA_EXCEEDED"
        );
        this.communityId = communityId;
        this.activeLeases = activeLeases;
        this.maxAllowed = maxAllowed;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public int getActiveLeases() {
        return activeLeases;
    }

    public int getMaxAllowed() {
        return maxAllowed;
    }
}
