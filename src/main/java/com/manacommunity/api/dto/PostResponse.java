package com.manacommunity.api.dto;

import com.manacommunity.api.model.PostType;
import com.manacommunity.api.model.PostVisibility;
import com.manacommunity.api.model.PostPriority;
import com.manacommunity.api.model.ReactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PostResponse(
    Long id,
    String content,
    String title,
    String imageUrl,
    boolean official,
    boolean pinned,
    int likesCount,
    int commentsCount,
    int sharesCount,
    int bookmarksCount,
    int viewsCount,
    boolean likedByCurrentUser,
    boolean bookmarkedByCurrentUser,
    ReactionType currentUserReaction,
    Map<String, Long> reactionCounts,
    Long authorId,
    String authorName,
    String authorAvatar,
    String authorRole,
    String authorProfilePic,
    LocalDateTime createdAt,
    PostType postType,
    PostVisibility visibility,
    PostPriority priority,
    Double price,
    String location,
    String pollQuestion,
    List<String> pollOptionsList,
    Map<String, Long> pollVotes,
    String userVotedOption,
    LocalDateTime pollEndDate,
    boolean pollAnonymous,
    String hashtags,
    String mentions,
    String linkUrl,
    String linkTitle,
    String linkDescription,
    String linkImage,
    LocalDateTime eventDate,
    LocalDateTime eventEndDate,
    String eventVenue,
    List<MediaResponse> media,
    GroupSummary group,
    String moderationStatus
) {
    public record MediaResponse(
        Long id,
        String mediaUrl,
        String mediaType,
        String thumbnailUrl,
        String altText,
        int sortOrder
    ) {}

    public record GroupSummary(
        Long id,
        String name,
        String slug,
        String iconUrl,
        String groupType
    ) {}
}
