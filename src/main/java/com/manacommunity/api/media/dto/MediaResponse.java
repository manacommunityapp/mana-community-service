package com.manacommunity.api.media.dto;

import com.manacommunity.api.media.entity.MediaModule;
import com.manacommunity.api.media.entity.MediaStatus;
import com.manacommunity.api.media.entity.MediaType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a media object.
 *
 * URLs (url, thumbnailUrl, compressedUrl, mediumUrl) are generated dynamically at
 * request time — they are presigned S3 GET URLs or CloudFront URLs.
 * They are NEVER stored in the database.
 */
@Data
@Builder
public class MediaResponse {

    private UUID id;

    private MediaModule module;
    private String moduleId;
    private Long communityId;
    private String subContext;

    private MediaType mediaType;
    private Long fileSize;
    private String mimeType;
    private String originalFileName;
    private String caption;
    private String altText;
    private boolean featured;
    private int sortOrder;
    private MediaStatus status;

    // ── Dynamically generated access URLs ─────────────────────────────────
    /** Original file access URL (presigned S3 or CloudFront). */
    private String url;
    /** Thumbnail access URL (presigned S3 or CloudFront). Null if not yet processed. */
    private String thumbnailUrl;
    /** Compressed/medium version URL. Null if not yet processed. */
    private String compressedUrl;
    /** 1920 px medium image URL. Null if not yet processed. */
    private String mediumUrl;

    // ── Dimensional metadata ───────────────────────────────────────────────
    private Integer width;
    private Integer height;
    private Integer durationSeconds;

    // ── Audit ─────────────────────────────────────────────────────────────
    private Long uploadedBy;
    private OffsetDateTime uploadedAt;
    private OffsetDateTime createdAt;
}
