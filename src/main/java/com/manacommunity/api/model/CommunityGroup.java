package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_group", indexes = {
    @Index(name = "idx_group_community", columnList = "community_id"),
    @Index(name = "idx_group_type", columnList = "group_type"),
    @Index(name = "idx_group_slug", columnList = "slug")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    @Column(name = "icon_url", length = 1000)
    private String iconUrl;

    @Column(name = "group_type", nullable = false, length = 30)
    @Builder.Default
    private String groupType = "PUBLIC";

    @Column(name = "category", length = 50)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "member_count", nullable = false)
    @Builder.Default
    private int memberCount = 0;

    @Column(name = "post_count", nullable = false)
    @Builder.Default
    private int postCount = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private boolean requiresApproval = false;

    @Column(name = "allow_member_posts", nullable = false)
    @Builder.Default
    private boolean allowMemberPosts = true;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "tags", length = 500)
    private String tags;

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
