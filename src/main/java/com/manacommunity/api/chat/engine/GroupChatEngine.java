package com.manacommunity.api.chat.engine;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class GroupChatEngine {

    public enum GroupRole {
        ADMIN, MODERATOR, MEMBER, MUTED
    }

    public record GroupMember(Long userId, GroupRole role) {}

    /**
     * Checks if a user has permission to post in a group channel.
     */
    public boolean canUserPost(Long userId, boolean isAnnouncementOnly, List<GroupMember> members) {
        if (userId == null || members == null) return false;

        GroupMember member = members.stream()
                .filter(m -> m.userId().equals(userId))
                .findFirst()
                .orElse(null);

        if (member == null || member.role() == GroupRole.MUTED) {
            return false;
        }

        if (isAnnouncementOnly) {
            return member.role() == GroupRole.ADMIN || member.role() == GroupRole.MODERATOR;
        }

        return true;
    }

    /**
     * Checks if a user has permission to add or remove members from a group.
     */
    public boolean canManageMembers(Long userId, List<GroupMember> members) {
        if (userId == null || members == null) return false;

        return members.stream()
                .anyMatch(m -> m.userId().equals(userId) && m.role() == GroupRole.ADMIN);
    }

    /**
     * Aggregates total unread badges across multiple conversation channels.
     */
    public long calculateTotalUnreadCount(List<Long> unreadPerChannel) {
        if (unreadPerChannel == null || unreadPerChannel.isEmpty()) return 0L;
        return unreadPerChannel.stream().filter(c -> c != null && c > 0).mapToLong(Long::longValue).sum();
    }
}
