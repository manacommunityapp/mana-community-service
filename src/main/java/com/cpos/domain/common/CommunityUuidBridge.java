package com.cpos.domain.common;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Bridges the legacy Long communityId primary key with CPOS UUID-based multi-tenancy.
 *
 * <p>Provides deterministic 1:1 bidirectional mapping so that community admins
 * (whose authentication principal holds a Long communityId) can access CPOS
 * resources scoped to their own community while preventing cross-community data access.</p>
 */
public final class CommunityUuidBridge {

    private CommunityUuidBridge() {}

    /**
     * Deterministically derives a standard RFC 4122 Type 3 UUID from a Long community ID.
     */
    public static UUID toUuid(Long communityId) {
        if (communityId == null) {
            return null;
        }
        return UUID.nameUUIDFromBytes(("mana:community:" + communityId).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a Long community ID into a zero-padded 64-bit UUID (most significant bits = 0).
     */
    public static UUID toPaddedUuid(Long communityId) {
        if (communityId == null) {
            return null;
        }
        return new UUID(0L, communityId);
    }

    /**
     * Returns true if the requestedUuid matches either the deterministic Type 3 UUID
     * or the zero-padded numerical UUID for the given community ID.
     */
    public static boolean matches(Long communityId, UUID requestedUuid) {
        if (communityId == null || requestedUuid == null) {
            return false;
        }
        if (requestedUuid.equals(toUuid(communityId))) {
            return true;
        }
        return requestedUuid.getMostSignificantBits() == 0L && requestedUuid.getLeastSignificantBits() == communityId.longValue();
    }
}
