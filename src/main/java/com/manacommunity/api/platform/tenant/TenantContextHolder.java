package com.manacommunity.api.platform.tenant;

/**
 * Holds the resolved community ID for the current request thread.
 * Populated by {@link TenantSecurityFilter} from the authenticated user's JWT claims.
 * Always cleared in a {@code finally} block so pooled threads don't leak state.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Long> COMMUNITY_ID = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void set(Long communityId) {
        COMMUNITY_ID.set(communityId);
    }

    public static Long get() {
        return COMMUNITY_ID.get();
    }

    /** Throws if no tenant context has been set — use in service methods that require isolation. */
    public static void require() {
        if (COMMUNITY_ID.get() == null) {
            throw new IllegalStateException("No tenant context — request must be authenticated");
        }
    }

    public static void clear() {
        COMMUNITY_ID.remove();
    }
}
