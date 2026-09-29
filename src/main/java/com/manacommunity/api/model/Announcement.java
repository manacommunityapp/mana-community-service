package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Admin-created community announcements pinned above the feed.
 * Priority URGENT gets highlighted styling in the mobile app.
 */
@Entity
@Table(name = "announcement", indexes = {
    @Index(name = "idx_ann_community_created", columnList = "community_id, created_at"),
    @Index(name = "idx_ann_community_pinned",  columnList = "community_id, is_pinned")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * NORMAL or URGENT — URGENT renders with red styling and alerts all members.
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String priority = "NORMAL";

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    /**
     * Optional expiry — mobile app hides the announcement after this datetime.
     * NULL means it stays visible until manually deleted.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
