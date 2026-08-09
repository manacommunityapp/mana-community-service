package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_auction_item", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAuctionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommunityEvent event;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Column(name = "current_bid", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal currentBid = BigDecimal.ZERO;

    @Column(name = "min_increment", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minIncrement = new BigDecimal("500");

    @Column(name = "image_emoji", length = 10)
    private String imageEmoji;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ItemStatus status = ItemStatus.UPCOMING;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "bid_count", nullable = false)
    @Builder.Default
    private Integer bidCount = 0;

    @Column(name = "leader_name", length = 200)
    private String leaderName;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ItemStatus {
        UPCOMING, LIVE, CLOSED
    }
}
