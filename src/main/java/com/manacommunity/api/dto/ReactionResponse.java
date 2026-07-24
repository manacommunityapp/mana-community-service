package com.manacommunity.api.dto;

import com.manacommunity.api.model.ReactionType;

import java.util.Map;

public record ReactionResponse(
    int totalReactions,
    Map<String, Long> reactionCounts,
    ReactionType currentUserReaction
) {}
