package com.manacommunity.api.noticeboard.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NoticeCategory category = NoticeCategory.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private NoticePriority priority = NoticePriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false, length = 20)
    @Builder.Default
    private TargetAudience targetAudience = TargetAudience.ALL;

    @Column(name = "target_block", length = 50)
    private String targetBlock; // Specific Tower/Block if applicable

    @Column(name = "pinned", nullable = false)
    @Builder.Default
    private boolean pinned = false;

    @Column(name = "requires_acknowledgement", nullable = false)
    @Builder.Default
    private boolean requiresAcknowledgement = false;

    @Column(name = "attachments", length = 2000)
    private String attachments; // PDF / circular URLs

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NoticeStatus status = NoticeStatus.PUBLISHED;

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum NoticeCategory { GENERAL, MAINTENANCE, SAFETY, EVENT, MEETING, RULE_CHANGE }
    public enum NoticePriority { LOW, NORMAL, HIGH, URGENT }
    public enum TargetAudience { ALL, OWNERS_ONLY, TENANTS_ONLY, BLOCK_SPECIFIC, COMMITTEE_ONLY }
    public enum NoticeStatus { DRAFT, SCHEDULED, PUBLISHED, EXPIRED, ARCHIVED }
}
