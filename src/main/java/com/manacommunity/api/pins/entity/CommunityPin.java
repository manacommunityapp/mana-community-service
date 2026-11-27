package com.manacommunity.api.pins.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "community_pins", schema = "manacommunity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser author;

    @Column(name = "author_name", nullable = false, length = 120)
    private String authorName;

    @Column(name = "flat_number", length = 30)
    private String flatNumber;

    @Column(nullable = false, length = 40)
    private String category; // ALERTS, SPORTS, HELP_NEEDED, OFFERS, CARPOOL, LOST_FOUND, GENERAL

    @Column(nullable = false, length = 280)
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "external_link", length = 500)
    private String externalLink;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private Boolean isClosed = false;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Integer likesCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
