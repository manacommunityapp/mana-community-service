package com.manacommunity.platform.privacy;

import org.springframework.stereotype.Component;

/**
 * Prevents health data from leaking into community-feed or public contexts.
 * Call {@link #assertNotCommunityFeedContext()} at the start of any method that
 * touches raw health records to ensure they are never served in a feed response.
 */
@Component
public class HealthDataIsolationGuard {

    private static final ThreadLocal<Boolean> COMMUNITY_FEED_CONTEXT = ThreadLocal.withInitial(() -> false);

    /** Marks the current thread as being inside a community-feed rendering context. */
    public static void enterCommunityFeedContext() {
        COMMUNITY_FEED_CONTEXT.set(true);
    }

    /** Clears the community-feed context flag. Must be called in a finally block. */
    public static void exitCommunityFeedContext() {
        COMMUNITY_FEED_CONTEXT.remove();
    }

    /**
     * Throws if the current thread is building a community-feed response.
     * Health records must never appear in community feeds.
     */
    public static void assertNotCommunityFeedContext() {
        if (Boolean.TRUE.equals(COMMUNITY_FEED_CONTEXT.get())) {
            throw new HealthDataInFeedException(
                    "Health data must not be exposed in community-feed context");
        }
    }

    public static class HealthDataInFeedException extends RuntimeException {
        public HealthDataInFeedException(String message) {
            super(message);
        }
    }
}
