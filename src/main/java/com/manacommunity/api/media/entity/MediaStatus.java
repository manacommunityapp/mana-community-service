package com.manacommunity.api.media.entity;

/** Lifecycle state of a media object. */
public enum MediaStatus {
    /** Waiting for admin approval (approval_required = true). */
    PENDING,
    /** Being processed (thumbnail generation, compression). */
    PROCESSING,
    /** Fully active and accessible. */
    ACTIVE,
    /** Rejected during approval workflow. */
    REJECTED,
    /** Soft-deleted — hidden from all reads, pending physical S3 cleanup. */
    DELETED,
    /** Archived to cold storage (Glacier). */
    ARCHIVED
}
