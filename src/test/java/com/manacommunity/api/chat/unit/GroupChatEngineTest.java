package com.manacommunity.api.chat.unit;

import com.manacommunity.api.chat.engine.GroupChatEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Group Chat & Permissions Engine Unit Tests")
class GroupChatEngineTest {

    private GroupChatEngine groupEngine;

    @BeforeEach
    void setUp() {
        groupEngine = new GroupChatEngine();
    }

    @Test
    @DisplayName("Should enforce announcement-only posting permissions")
    void shouldEnforceAnnouncementPosting() {
        var admin = new GroupChatEngine.GroupMember(1L, GroupChatEngine.GroupRole.ADMIN);
        var member = new GroupChatEngine.GroupMember(2L, GroupChatEngine.GroupRole.MEMBER);
        var muted = new GroupChatEngine.GroupMember(3L, GroupChatEngine.GroupRole.MUTED);
        var members = List.of(admin, member, muted);

        // In announcement only group: Admin can post, regular member cannot
        assertTrue(groupEngine.canUserPost(1L, true, members));
        assertFalse(groupEngine.canUserPost(2L, true, members));

        // In standard group: Member can post, muted user cannot
        assertTrue(groupEngine.canUserPost(2L, false, members));
        assertFalse(groupEngine.canUserPost(3L, false, members));
    }

    @Test
    @DisplayName("Should verify admin rights to manage members")
    void shouldVerifyManageMembers() {
        var admin = new GroupChatEngine.GroupMember(1L, GroupChatEngine.GroupRole.ADMIN);
        var member = new GroupChatEngine.GroupMember(2L, GroupChatEngine.GroupRole.MEMBER);

        assertTrue(groupEngine.canManageMembers(1L, List.of(admin, member)));
        assertFalse(groupEngine.canManageMembers(2L, List.of(admin, member)));
    }

    @Test
    @DisplayName("Should calculate total unread badge count")
    void shouldCalculateTotalUnreadCount() {
        long total = groupEngine.calculateTotalUnreadCount(List.of(3L, 0L, 5L, 2L));
        assertEquals(10L, total);
        assertEquals(0L, groupEngine.calculateTotalUnreadCount(List.of()));
    }
}
