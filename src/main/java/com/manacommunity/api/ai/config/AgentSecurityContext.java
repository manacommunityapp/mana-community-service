package com.manacommunity.api.ai.config;

/**
 * Thread-local holder for the validated user context during an AI agent request.
 *
 * <p>Set at the start of each chat request by {@code AiChatAgentService},
 * read by every {@code @Tool} method to enforce community-scoped data isolation,
 * and cleared in a {@code finally} block.</p>
 *
 * <p>This is NOT a replacement for Spring Security — the HTTP layer still validates
 * the JWT. This context carries the <em>resolved</em> user identity (community, role,
 * team ownership) so tool methods don't need to re-query the user on every call.</p>
 */
public final class AgentSecurityContext {

    private AgentSecurityContext() {}

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext get() {
        UserContext ctx = CONTEXT.get();
        if (ctx == null) {
            throw new SecurityException("AgentSecurityContext not initialized — "
                    + "tool called outside a valid chat request.");
        }
        return ctx;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Immutable snapshot of the authenticated user's identity, resolved once per
     * chat request from the database.
     *
     * @param userId        the user's database ID
     * @param communityId   the user's community ID (never null for valid requests)
     * @param role          the user's role (SUPER_ADMIN, ADMIN, MEMBER, etc.)
     * @param teamId        if the user owns an auction team, its ID; otherwise null
     * @param fullName      the user's display name
     * @param kycVerified   whether KYC status is "VERIFIED"
     */
    public record UserContext(
            Long userId,
            Long communityId,
            String role,
            Long teamId,
            String fullName,
            boolean kycVerified
    ) {
        public boolean isAdmin() {
            return "SUPER_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
        }

        public boolean isSuperAdmin() {
            return "SUPER_ADMIN".equalsIgnoreCase(role);
        }

        public boolean ownsTeam(Long checkTeamId) {
            return teamId != null && teamId.equals(checkTeamId);
        }
    }
}
