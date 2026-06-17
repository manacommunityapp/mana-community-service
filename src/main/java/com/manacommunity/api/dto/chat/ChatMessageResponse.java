package com.manacommunity.api.dto.chat;

import java.time.LocalDateTime;

/**
 * A message as returned to the client. The frontend decides "sent" vs
 * "received" by comparing {@code senderId} against the logged-in user.
 */
public record ChatMessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String type,
        String content,
        LocalDateTime createdAt
) {}
