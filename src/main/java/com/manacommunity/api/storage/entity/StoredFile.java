package com.manacommunity.api.storage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Binary file stored in PostgreSQL (active when app.storage.type=postgres).
 * When S3 is configured this table exists but is not written to;
 * files live in the S3 bucket and their URLs are stored directly on the
 * owning entity (e.g. event_invoice.invoice_url).
 */
@Entity
@Table(name = "stored_file", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.BINARY)
    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] data;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    /**
     * Owning community (tenant). Added in V506 and back-filled from
     * {@code app_user.community_id} via {@code uploaded_by}.
     *
     * <p>Nullable: rows uploaded without an authenticated user — or by a user who has
     * since been removed, or who belongs to no community — have no derivable tenant.
     * Those rows stay NULL and are SUPER_ADMIN-only, see
     * {@code FileStorageController#canAccess}.
     */
    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
