package com.manacommunity.api.marketplace.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "marketplace_listings", indexes = {
    @Index(name = "idx_mkt_listing_comm", columnList = "community_id"),
    @Index(name = "idx_mkt_listing_status", columnList = "status"),
    @Index(name = "idx_mkt_listing_cat", columnList = "category")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 20)
    @Builder.Default
    private String priceUnit = "INR";

    @Column(nullable = false, length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Condition condition;

    @Column(length = 100)
    private String warranty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private TransactionMode transactionMode = TransactionMode.IN_APP_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ListingVisibility visibility = ListingVisibility.COMMUNITY;

    @Column(length = 100)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private AppUser seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<MarketListingImage> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ListingStatus.ACTIVE;
        if (visibility == null) visibility = ListingVisibility.COMMUNITY;
        if (priceUnit == null) priceUnit = "INR";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Condition { NEW, LIKE_NEW, GOOD, FAIR }
    public enum ListingStatus { ACTIVE, PENDING, SOLD, PAUSED, DELETED }
    public enum TransactionMode { CHAT_ONLY, CHAT_CALL, IN_APP_PAYMENT, BARTER, FREE }
    public enum ListingVisibility { COMMUNITY, NEARBY }
}
