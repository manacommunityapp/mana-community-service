package com.manacommunity.api.media.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Returned when a client requests a presigned S3 PUT URL.
 * Client flow:
 *   1. POST /api/media/presigned-upload → receive this DTO
 *   2. PUT presignedUploadUrl (directly to S3 — no app server)
 *   3. POST /api/media/presigned-upload/confirm/{sessionId}
 */
@Data
@Builder
public class PresignedUrlResponse {

    /** Session token — required to confirm upload after the PUT. */
    private UUID sessionId;

    /** Pre-signed S3 PUT URL. Valid only for {@code expiresAt}. */
    private String presignedUploadUrl;

    /** HTTP method to use — always PUT. */
    private String httpMethod;

    /** The S3 key where the file will be stored. */
    private String s3Key;

    /** MIME type the client MUST set as Content-Type header in the PUT request. */
    private String contentType;

    /** When the presigned URL expires. */
    private OffsetDateTime expiresAt;
}
