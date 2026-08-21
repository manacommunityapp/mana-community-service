package com.manacommunity.api.dto;

import java.util.Map;

/**
 * Returned after a user toggles (add/change/remove) a rich comment reaction.
 *
 * @param commentId       the comment that was reacted to
 * @param likesCount      total reaction count (all types combined)
 * @param userReaction    the current user's reaction type name, or null if they un-reacted
 * @param reactionCounts  per-type counts, e.g. { "LIKE": 3, "LOVE": 2 }
 */
public record CommentReactionToggleResponse(
    Long commentId,
    int likesCount,
    String userReaction,
    Map<String, Long> reactionCounts
) {}