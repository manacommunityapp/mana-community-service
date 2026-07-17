package com.manacommunity.api.dto;

import java.time.LocalDateTime;

import com.manacommunity.api.model.PostType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PostResponse(
    Long id,
    String content,
    String imageUrl,
    boolean official,
    int likesCount,
    int commentsCount,
    boolean likedByCurrentUser,
    Long authorId,
    String authorName,
    String authorAvatar,
    String authorRole,
    LocalDateTime createdAt,
    PostType postType,
    Double price,
    String location,
    String pollQuestion,
    List<String> pollOptionsList,
    Map<String, Long> pollVotes,
    String userVotedOption
) {}
