package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records a user's report of inappropriate content (post, comment, or user).
 * Admins review these via the admin panel moderation queue.
 */
@Entity
@Table(name = "content_report", indexes = {
    @Index(name = "idx_report_community_status", columnList = "community_id, status"),
    @Index(name = "idx_report_target",            columnList = "target_type, target_id"),
    @Index(name = "idx_report_reporter",          columnList = "reporter_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private AppUser reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    /**
     * What is being reported: POST, COMMENT, USER
     */
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    /**
     * The ID of the reported object (post.id, comment.id, or user.id).
     */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * Short excerpt of the reported content (max 500 chars), stored for
     * quick review without re-fetching the original object.
     */
    @Column(name = "target_content", length = 500)
    private String targetContent;

    /**
     * Name of the author of the reported content (denormalised for quick display).
     */
    @Column(name = "target_author", length = 100)
    private String targetAuthor;

    /**
     * Reporter-supplied reason: SPAM, HARASSMENT, INAPPROPRIATE, MISINFORMATION, OTHER
     */
    @Column(nullable = false, length = 40)
    private String reason;

    /**
     * Lifecycle: PENDING → RESOLVED or DISMISSED
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private AppUser resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
