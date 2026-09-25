package com.manacommunity.api.dto;

import com.manacommunity.api.model.NotificationCategory;
import com.manacommunity.api.model.NotificationPriority;
import com.manacommunity.api.model.NotificationType;
import com.manacommunity.api.model.ReferenceType;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for user notification inbox & feed bell.
 * Contains only the fields required for UI rendering, skipping metadata, channel logs, and audit internals.
 */
public record NotificationSummaryResponse(
    Long id,
    String type,
    String category,
    String title,
    String body,
    String icon,
    String actionUrl,
    String referenceType,
    Long referenceId,
    String priority,
    boolean read,
    LocalDateTime createdAt
) {
    public NotificationSummaryResponse(
        Long id,
        NotificationType type,
        NotificationCategory category,
        String title,
        String body,
        String icon,
        String actionUrl,
        ReferenceType referenceType,
        Long referenceId,
        NotificationPriority priority,
        boolean read,
        LocalDateTime createdAt
    ) {
        this(
            id,
            type != null ? type.name() : null,
            category != null ? category.name() : null,
            title,
            body,
            icon,
            actionUrl,
            referenceType != null ? referenceType.name() : null,
            referenceId,
            priority != null ? priority.name() : null,
            read,
            createdAt
        );
    }
}