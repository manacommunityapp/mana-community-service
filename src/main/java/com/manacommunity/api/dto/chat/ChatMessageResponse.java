package com.manacommunity.api.dto.chat;

import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime createdAt,
        List<ChatAttachmentResponse> attachments
) {
    /** Backwards-compatible constructor for messages with no attachments. */
    public ChatMessageResponse(Long id, Long conversationId, Long senderId,
                               String senderName, String type, String content,
                               LocalDateTime createdAt) {
        this(id, conversationId, senderId, senderName, type, content, createdAt, List.of());
    }
}
