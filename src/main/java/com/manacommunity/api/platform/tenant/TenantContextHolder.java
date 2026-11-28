package com.manacommunity.api.platform.tenant;

/**
 * Holds the resolved community ID for the current request thread.
 * Populated by {@link TenantSecurityFilter} from the authenticated user's JWT claims.
 * Always cleared in a {@code finally} block so pooled threads don't leak state.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Long> COMMUNITY_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> TOWER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> FLAT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void set(Long communityId) {
        COMMUNITY_ID.set(communityId);
    }

    public static void setContext(Long communityId, Long towerId, Long flatId, Long userId) {
        COMMUNITY_ID.set(communityId);
        TOWER_ID.set(towerId);
        FLAT_ID.set(flatId);
        USER_ID.set(userId);
    }

    public static Long get() {
        return COMMUNITY_ID.get();
    }

    public static Long getTowerId() {
        return TOWER_ID.get();
    }

    public static Long getFlatId() {
        return FLAT_ID.get();
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    /** Throws if no tenant context has been set — use in service methods that require isolation. */
    public static void require() {
        if (COMMUNITY_ID.get() == null) {
            throw new IllegalStateException("No tenant context — request must be authenticated");
        }
    }

    public static void clear() {
        COMMUNITY_ID.remove();
        TOWER_ID.remove();
        FLAT_ID.remove();
        USER_ID.remove();
    }
}
