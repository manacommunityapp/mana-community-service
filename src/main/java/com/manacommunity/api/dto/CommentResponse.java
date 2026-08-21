package com.manacommunity.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record CommentResponse(
    Long id,
    Long postId,
    String content,
    Long authorId,
    String authorName,
    String authorAvatar,
    String authorRole,
    String authorProfilePic,
    LocalDateTime createdAt,
    Long parentId,
    int likesCount,
    int repliesCount,
    boolean pinned,
    boolean acceptedAnswer,
    List<CommentResponse> replies,
    boolean likedByCurrentUser,
    /** The current user's comment reaction type (e.g. "LOVE"), or null if they haven't reacted. */
    String userCommentReaction,
    /** Per-type reaction counts, e.g. { "LIKE": 3, "LOVE": 2, "CELEBRATE": 1 } */
    Map<String, Long> commentReactionCounts
) {
    /** Backward-compat constructor: no reaction info (used by tests and older callers) */
    public CommentResponse(Long id, Long postId, String content, Long authorId, String authorName,
                           String authorAvatar, String authorRole, String authorProfilePic,
                           LocalDateTime createdAt, Long parentId, int likesCount, int repliesCount,
                           boolean pinned, boolean acceptedAnswer, List<CommentResponse> replies) {
        this(id, postId, content, authorId, authorName, authorAvatar, authorRole, authorProfilePic,
                createdAt, parentId, likesCount, repliesCount, pinned, acceptedAnswer, replies,
                false, null, null);
    }

    /** Constructor with likedByCurrentUser but no rich reaction info */
    public CommentResponse(Long id, Long postId, String content, Long authorId, String authorName,
                           String authorAvatar, String authorRole, String authorProfilePic,
                           LocalDateTime createdAt, Long parentId, int likesCount, int repliesCount,
                           boolean pinned, boolean acceptedAnswer, List<CommentResponse> replies,
                           boolean likedByCurrentUser) {
        this(id, postId, content, authorId, authorName, authorAvatar, authorRole, authorProfilePic,
                createdAt, parentId, likesCount, repliesCount, pinned, acceptedAnswer, replies,
                likedByCurrentUser, null, null);
    }
}