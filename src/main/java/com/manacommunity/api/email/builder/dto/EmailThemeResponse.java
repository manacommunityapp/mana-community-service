package com.manacommunity.api.email.builder.dto;

import com.manacommunity.api.email.builder.entity.EmailThemeConfig;

import java.time.LocalDateTime;

public record EmailThemeResponse(
        Long id,
        Long communityId,
        String name,
        String themeJson,
        Boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EmailThemeResponse from(EmailThemeConfig entity) {
        return new EmailThemeResponse(
                entity.getId(),
                entity.getCommunityId(),
                entity.getName(),
                entity.getThemeJson(),
                entity.getIsDefault(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
