package com.manacommunity.api.marketplace.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_donations", indexes = {
    @Index(name = "idx_mkt_don_comm", columnList = "community_id"),
    @Index(name = "idx_mkt_don_status", columnList = "status"),
    @Index(name = "idx_mkt_don_sharing_mode", columnList = "sharing_mode")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDonation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ItemCondition condition = ItemCondition.GOOD;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_mode", length = 30)
    @Builder.Default
    private SharingMode sharingMode = SharingMode.GIVEAWAY;

    @Column(name = "barter_preferred_item", length = 200)
    private String barterPreferredItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DonationStatus status = DonationStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private AppUser donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by_id")
    private AppUser claimedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = DonationStatus.AVAILABLE;
        if (condition == null) condition = ItemCondition.GOOD;
        if (sharingMode == null) sharingMode = SharingMode.GIVEAWAY;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ItemCondition { NEW, LIKE_NEW, GOOD, FAIR }
    public enum SharingMode { GIVEAWAY, BARTER_EXCHANGE, BORROW_RETURN }
    public enum DonationStatus { AVAILABLE, CLAIMED, DONATED }
}
