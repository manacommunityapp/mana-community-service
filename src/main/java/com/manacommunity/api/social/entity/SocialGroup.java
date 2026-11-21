package com.manacommunity.api.social.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "social_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocialGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "group_type", length = 50)
    @Builder.Default
    private String groupType = "INTEREST";

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "is_private")
    @Builder.Default
    private boolean isPrivate = false;

    @Column(name = "max_members")
    @Builder.Default
    private int maxMembers = 50;

    @Column(name = "member_count")
    @Builder.Default
    private int memberCount = 1;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
