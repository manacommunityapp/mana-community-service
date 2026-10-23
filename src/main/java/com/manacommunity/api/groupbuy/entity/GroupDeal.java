package com.manacommunity.api.groupbuy.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_deal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 80)
    private String subCategory;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(precision = 12, scale = 2)
    private BigDecimal mrp;

    @Column(length = 150)
    private String vendorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private AppUser vendor;

    @Column(precision = 3, scale = 1)
    private BigDecimal vendorRating;

    @Column(nullable = false)
    @Builder.Default
    private Boolean vendorVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer targetParticipants = 10;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentParticipants = 0;

    @Column(nullable = false)
    private LocalDateTime endDate;

    private LocalDateTime deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DealStatus status = DealStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFestival = false;

    @Column(length = 100)
    private String festivalName;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String pickupPoints;

    @OneToMany(mappedBy = "deal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<GroupDealTier> tiers = new ArrayList<>();

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

    public enum DealStatus { ACTIVE, FUNDED, FULFILLED, CANCELLED, EXPIRED }
}
