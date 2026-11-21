package com.manacommunity.api.search;

import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "search_index")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private SearchModule module;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Stored as comma-separated tags (avoids array type mapping complexity)
    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "deep_link_type", nullable = false, length = 60)
    private String deepLinkType;

    @Column(name = "deep_link_id", nullable = false, length = 100)
    private String deepLinkId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private SearchVisibility visibility = SearchVisibility.COMMUNITY;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    // JSON stored as text — no extra hypersistence dependency needed
    @Column(columnDefinition = "TEXT")
    private String metadata;

    // search_vector is TSVECTOR managed by DB trigger — NOT mapped as a JPA field
    // to avoid trigger re-firing on every save

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "indexed_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant indexedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
