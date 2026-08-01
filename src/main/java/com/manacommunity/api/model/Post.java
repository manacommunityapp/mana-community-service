package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post", indexes = {
    @Index(name = "idx_post_community_created", columnList = "community_id, created_at DESC"),
    @Index(name = "idx_post_type", columnList = "post_type"),
    @Index(name = "idx_post_group", columnList = "group_id"),
    @Index(name = "idx_post_pinned", columnList = "is_pinned"),
    @Index(name = "idx_post_visibility", columnList = "visibility")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private CommunityGroup group;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_official", nullable = false)
    private boolean official;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private boolean pinned = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false)
    private PostType postType = PostType.GENERAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private PostVisibility visibility = PostVisibility.COMMUNITY;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private PostPriority priority = PostPriority.NORMAL;

    @Column(name = "price")
    private Double price;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "poll_question", length = 500)
    private String pollQuestion;

    @Column(name = "poll_options", length = 1000)
    private String pollOptions;

    @Column(name = "poll_end_date")
    private LocalDateTime pollEndDate;

    @Column(name = "poll_anonymous")
    @Builder.Default
    private boolean pollAnonymous = false;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private int likesCount = 0;

    @Column(name = "comments_count", nullable = false)
    @Builder.Default
    private int commentsCount = 0;

    @Column(name = "shares_count", nullable = false)
    @Builder.Default
    private int sharesCount = 0;

    @Column(name = "bookmarks_count", nullable = false)
    @Builder.Default
    private int bookmarksCount = 0;

    @Column(name = "views_count", nullable = false)
    @Builder.Default
    private int viewsCount = 0;

    @Column(name = "hashtags", length = 1000)
    private String hashtags;

    @Column(name = "mentions", length = 1000)
    private String mentions;

    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    @Column(name = "link_title", length = 500)
    private String linkTitle;

    @Column(name = "link_description", length = 1000)
    private String linkDescription;

    @Column(name = "link_image", length = 1000)
    private String linkImage;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "event_end_date")
    private LocalDateTime eventEndDate;

    @Column(name = "event_venue", length = 500)
    private String eventVenue;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "moderation_status", length = 20)
    @Builder.Default
    private String moderationStatus = "APPROVED";

    @Column(name = "moderation_note", length = 1000)
    private String moderationNote;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostMedia> media = new ArrayList<>();

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
}
