package com.manacommunity.api.document.dto;

import com.manacommunity.api.document.entity.DocAccessLevel;
import com.manacommunity.api.document.entity.DocumentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DocumentUploadRequest(
        @NotBlank String title,
        String description,
        @NotNull DocumentCategory category,
        String entityType,
        String entityId,
        DocAccessLevel accessLevel,
        LocalDate expiresAt,
        Integer reminderDaysBefore,
        Integer retentionDays
) {}
