package com.manacommunity.platform.websocket;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Central registry of all protected WebSocket topic patterns.
 * Unknown topics that look like /topic/ or /queue/ are DENIED (fail-closed).
 */
public final class TopicRegistry {

    public enum TopicPermission {
        SELF_ONLY,        // only the user whose ID is in the destination
        COMMUNITY_MEMBER, // any member of the community in the destination
        CHAT_MEMBER,      // participant in the chat/conversation
        ADMIN_ROLE,       // community admin or platform admin
        PUBLIC            // authenticated but no further check
    }

    /** Ordered map: pattern → permission. First match wins. */
    private static final Map<Pattern, TopicPermission> REGISTRY = new LinkedHashMap<>();

    static {
        // User-scoped topics
        register("/topic/chat-user/(\\d+)(?:/.*)?$",    TopicPermission.SELF_ONLY);
        register("/topic/notifications/(\\d+)(?:/.*)?$", TopicPermission.SELF_ONLY);
        register("/topic/user/(\\d+)(?:/.*)?$",          TopicPermission.SELF_ONLY);
        register("/queue/.*",                            TopicPermission.SELF_ONLY);

        // Community-scoped topics
        register("/topic/community/(\\d+)(?:/.*)?$",    TopicPermission.COMMUNITY_MEMBER);
        register("/topic/broadcast/(\\d+)(?:/.*)?$",    TopicPermission.COMMUNITY_MEMBER);
        register("/topic/emergency/(\\d+)(?:/.*)?$",    TopicPermission.COMMUNITY_MEMBER);
        register("/topic/noticeboard/(\\d+)(?:/.*)?$",  TopicPermission.COMMUNITY_MEMBER);
        register("/topic/sports/live(?:/.*)?$",          TopicPermission.COMMUNITY_MEMBER);

        // Social group chat topics
        register("/topic/groups/(\\d+)/messages$",       TopicPermission.COMMUNITY_MEMBER);

        // Chat/conversation topics
        register("/topic/conversation/(\\d+)(?:/.*)?$", TopicPermission.CHAT_MEMBER);

        // Resource topics (community-membership check via resource)
        register("/topic/auction/(\\d+)(?:/.*)?$",      TopicPermission.COMMUNITY_MEMBER);
        register("/topic/match/(\\d+)(?:/.*)?$",        TopicPermission.COMMUNITY_MEMBER);
        register("/topic/kitty-games/(\\d+)(?:/.*)?$",  TopicPermission.COMMUNITY_MEMBER);

        // Admin-only topics
        register("/topic/admin(?:/.*)?$",               TopicPermission.ADMIN_ROLE);
    }

    private static void register(String pattern, TopicPermission permission) {
        REGISTRY.put(Pattern.compile("^" + pattern), permission);
    }

    public static boolean isProtected(String destination) {
        return destination != null &&
               (destination.startsWith("/topic/") || destination.startsWith("/queue/"));
    }

    /** Returns the required permission, or null if the destination is not in the registry. */
    public static TopicPermission permissionFor(String destination) {
        if (destination == null) return null;
        for (var entry : REGISTRY.entrySet()) {
            if (entry.getKey().matcher(destination).find()) {
                return entry.getValue();
            }
        }
        return null; // unknown → caller should deny
    }

    private TopicRegistry() {}
}
