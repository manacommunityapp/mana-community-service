package com.manacommunity.api.dto;

import java.time.LocalDateTime;

public record GroupResponse(
    Long id,
    String name,
    String slug,
    String description,
    String coverImageUrl,
    String iconUrl,
    String groupType,
    String category,
    int memberCount,
    int postCount,
    boolean requiresApproval,
    boolean allowMemberPosts,
    String rules,
    String tags,
    Long createdById,
    String createdByName,
    boolean isMember,
    String memberRole,
    LocalDateTime createdAt
) {}
