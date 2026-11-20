package com.manacommunity.api.platform.tenant;

import org.junit.jupiter.api.AfterEach;

/**
 * Base class for cross-community isolation tests.
 * Subclasses set up data for COMMUNITY_A and verify that queries run in the
 * context of COMMUNITY_B return empty results, ensuring no data leakage.
 */
public abstract class CrossCommunityTestBase {

    protected static final Long COMMUNITY_A = 1L;
    protected static final Long COMMUNITY_B = 2L;

    protected void setTenantContext(Long communityId) {
        TenantContextHolder.set(communityId);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    /** Subclasses implement this to assert that community B cannot see community A's data. */
    protected abstract void assertCommunityBCannotSeeA();
}
