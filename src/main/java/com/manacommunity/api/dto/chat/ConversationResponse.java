package com.manacommunity.api.dto.chat;

import java.time.LocalDateTime;

/**
 * One row in the conversations list. For DIRECT chats {@code contact} is the
 * other participant; for GROUP chats {@code title}/{@code isGroup} apply.
 */
public record ConversationResponse(
        Long id,
        String type,
        String title,
        boolean isGroup,
        ChatContactResponse contact,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {}
