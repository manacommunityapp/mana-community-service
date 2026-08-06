package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record GroupRequest(
    @NotBlank String name,
    String description,
    String coverImageUrl,
    String iconUrl,
    String groupType,
    String category,
    Boolean requiresApproval,
    Boolean allowMemberPosts,
    String rules,
    String tags
) {}
