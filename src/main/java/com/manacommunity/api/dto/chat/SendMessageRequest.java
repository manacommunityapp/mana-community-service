package com.manacommunity.api.dto.chat;

/** Body for POST /api/chat/conversations/{id}/messages. */
public record SendMessageRequest(String content) {}
