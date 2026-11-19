package com.manacommunity.api.document.dto;

import com.manacommunity.api.document.entity.DocScanStatus;

import java.time.Instant;

public record DocumentVersionDto(
        Long id,
        int versionNumber,
        String originalFilename,
        String contentType,
        Long fileSizeBytes,
        DocScanStatus scanStatus,
        Instant uploadedAt,
        Long uploadedBy
) {}
