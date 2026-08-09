package com.manacommunity.api.media.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks a presigned-PUT upload session.
 * Created when a client requests a presigned URL for direct S3 upload (video / large files).
 * Confirmed by the client after successful PUT — triggers MediaObject creation.
 */
@Entity
@Table(
        name = "media_upload_sessions",
        schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(name = "uq_upload_session_id", columnNames = "session_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, updatable = false)
    @Builder.Default
    private UUID sessionId = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 50)
    private MediaModule module;

    @Column(name = "module_id", nullable = false, length = 100)
    private String moduleId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "sub_context", length = 100)
    private String subContext;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    /** The S3 key that was embedded in the presigned URL. */
    @Column(name = "presigned_key", nullable = false, length = 1024)
    private String presignedKey;

    @Column(name = "bucket", nullable = false, length = 255)
    private String bucket;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    @Column(name = "original_name", length = 512)
    private String originalName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (sessionId == null) sessionId = UUID.randomUUID();
    }
}
