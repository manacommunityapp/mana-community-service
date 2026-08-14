package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_gallery_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventGalleryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommunityEvent event;

    @Column(nullable = false, length = 500)
    private String url;

    /**
     * Optional reference to the centralized MediaObject UUID.
     * When set, EventGalleryService uses MediaUrlService to generate fresh
     * presigned / CloudFront URLs at read time — preventing expired URL errors.
     */
    @Column(name = "media_external_id")
    private java.util.UUID mediaExternalId;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 20)
    @Builder.Default
    private MediaType mediaType = MediaType.PHOTO;

    @Column(name = "album_name", length = 100)
    private String albumName;

    @Column(name = "day_tag", length = 100)
    private String dayTag;

    @Column(length = 100)
    private String category;

    @Column(length = 300)
    private String caption;

    @Builder.Default
    private boolean featured = false;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private AppUser uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum MediaType {
        PHOTO, VIDEO
    }
}
