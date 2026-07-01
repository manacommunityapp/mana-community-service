package com.manacommunity.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound request for the AI chat endpoint.
 *
 * @param message         the user's natural-language question (required, max 5 000 chars)
 * @param conversationId  optional — groups messages into a memory-tracked conversation;
 *                        omit to start a new conversation each time
 * @param auctionConfigId optional — if set, tools will default to this auction context
 *                        so the user doesn't have to specify it in every question
 */
public record AiChatRequest(

        @NotBlank(message = "Message cannot be empty")
        @Size(max = 5000, message = "Message too long (max 5 000 characters)")
        String message,

        Long conversationId,

        Long auctionConfigId
) {}
