package com.manacommunity.api.document.dto;

import com.manacommunity.api.document.entity.*;

import java.time.Instant;

public record DocumentDto(
        Long id,
        String title,
        String description,
        DocumentCategory category,
        String entityType,
        String entityId,
        DocumentStatus status,
        DocAccessLevel accessLevel,
        Instant expiresAt,
        boolean expiryWarning,
        int versionCount,
        String currentVersionFilename,
        Long fileSizeBytes,
        DocScanStatus scanStatus,
        Instant createdAt,
        Long createdBy
) {}
