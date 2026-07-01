package com.manacommunity.api.ai.dto;

import java.time.Instant;

/**
 * Outbound response from the AI chat endpoint.
 *
 * @param conversationId  echoed back (or generated) for conversation continuity
 * @param reply           the AI-generated response text
 * @param timestamp       server timestamp of the response
 */
public record AiChatResponse(
        Long conversationId,
        String reply,
        Instant timestamp
) {
    public AiChatResponse(Long conversationId, String reply) {
        this(conversationId, reply, Instant.now());
    }
}
