package com.manacommunity.api.dto.chat;

/**
 * A user the caller can chat with (other party in a DIRECT thread, or a
 * pickable member in the "start new chat" list).
 */
public record ChatContactResponse(
        Long id,
        String name,
        String role,
        String avatarInitials,
        boolean isOnline,
        boolean isVerified
) {}
