package com.manacommunity.api.media.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Stores metadata for every media file managed by the centralized Media Service.
 *
 * IMPORTANT:
 *  - Files are NEVER stored in this table. Only S3 keys.
 *  - Full S3/CloudFront URLs are NEVER persisted. They are generated dynamically
 *    by {@link com.manacommunity.api.media.service.MediaUrlService} at read time.
 *  - Soft delete: set {@code deleted = true}. Physical S3 deletion is async.
 */
@Entity
@Table(
        name = "media_objects",
        schema = "manacommunity",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_media_external_id", columnNames = "external_id"),
                @UniqueConstraint(name = "uq_media_s3_key",      columnNames = "s3_key")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public-facing UUID — safe to expose in API responses (hides surrogate key). */
    @Column(name = "external_id", nullable = false, updatable = false)
    @Builder.Default
    private UUID externalId = UUID.randomUUID();

    // ── Module context ───────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 50)
    private MediaModule module;

    /** Primary entity ID (e.g. eventId, userId, listingId). */
    @Column(name = "module_id", nullable = false, length = 100)
    private String moduleId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    /** Secondary context within the module (gallery, banner, sponsor, kyc …). */
    @Column(name = "sub_context", length = 100)
    private String subContext;

    // ── File identity ────────────────────────────────────────────────────────

    /** Original name supplied by the uploader — for display only, never used in storage path. */
    @Column(name = "original_file_name", nullable = false, length = 512)
    private String originalFileName;

    /** UUID-based stored name (no original filename). */
    @Column(name = "stored_file_name", nullable = false, length = 512)
    private String storedFileName;

    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType;

    @Column(name = "extension", nullable = false, length = 20)
    private String extension;

    /** File size in bytes. */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // ── Media classification ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    // ── S3 keys — NEVER store full URLs ──────────────────────────────────────

    @Column(name = "bucket_name", nullable = false, length = 255)
    private String bucketName;

    /** S3 key for the original/full-quality file. */
    @Column(name = "s3_key", nullable = false, length = 1024)
    private String s3Key;

    /** S3 key for the 400 px thumbnail (images) or first-frame JPEG (video). */
    @Column(name = "thumbnail_key", length = 1024)
    private String thumbnailKey;

    /** S3 key for the compressed version (720p H.264 for video / WebP for image). */
    @Column(name = "compressed_key", length = 1024)
    private String compressedKey;

    /** S3 key for the 1920 px medium image. */
    @Column(name = "medium_key", length = 1024)
    private String mediumKey;

    // ── Dimensional metadata ─────────────────────────────────────────────────

    private Integer width;
    private Integer height;
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // ── Status ───────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MediaStatus status = MediaStatus.ACTIVE;

    @Column(name = "processing_status", length = 30)
    @Builder.Default
    private String processingStatus = "COMPLETE";

    // ── Approval ─────────────────────────────────────────────────────────────

    @Column(name = "approval_required", nullable = false)
    @Builder.Default
    private boolean approvalRequired = false;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // ── Ownership ─────────────────────────────────────────────────────────────

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime uploadedAt = OffsetDateTime.now();

    // ── Soft delete ───────────────────────────────────────────────────────────

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // ── Enrichment ────────────────────────────────────────────────────────────

    @Column(name = "caption")
    private String caption;

    @Column(name = "alt_text", length = 512)
    private String altText;

    @Column(name = "featured", nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    // ── Timestamps ────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (uploadedAt == null) uploadedAt = now;
        if (externalId == null) externalId = UUID.randomUUID();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
