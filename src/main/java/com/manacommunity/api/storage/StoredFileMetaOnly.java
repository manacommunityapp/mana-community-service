package com.manacommunity.api.storage;

/**
 * Lightweight file metadata projection without binary BYTEA payload.
 */
public record StoredFileMetaOnly(
        Long id,
        String originalName,
        String contentType,
        Long sizeBytes
) {}
