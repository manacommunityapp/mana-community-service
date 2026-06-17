package com.manacommunity.api.dto.chat;

/** Body for POST /api/chat/conversations/direct — the other user's id. */
public record StartDirectRequest(Long userId) {}
