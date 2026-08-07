package com.manacommunity.api.storage;

/**
 * Lightweight transfer object returned after a successful file upload.
 *
 * @param id          database row id (null when stored in S3 — the URL is the identifier)
 * @param url         resolvable URL: "/api/files/{id}" for Postgres, full S3 URL for S3 storage
 * @param originalName original filename as uploaded by the client
 * @param contentType MIME type
 * @param sizeBytes   file size in bytes
 */
public record StoredFileDto(
        Long id,
        String url,
        String originalName,
        String contentType,
        Long sizeBytes
) {}
