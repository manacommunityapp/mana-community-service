package com.manacommunity.api.groupbuying.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_buy_deal")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDeal extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 80)
    private String subCategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "vendor_name", nullable = false, length = 120)
    private String vendor;

    @Column(name = "vendor_id", length = 60)
    private String vendorId;

    @Column(name = "vendor_rating")
    private Double vendorRating;

    @Column(name = "vendor_verified")
    private Boolean vendorVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false)
    private PricingModel pricingModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false)
    private PricingType pricingType;

    @Column(name = "mrp", precision = 12, scale = 2, nullable = false)
    private BigDecimal mrp;

    @Column(name = "standard_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal standardPrice;

    @Column(name = "current_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal currentPrice;

    @Column(name = "current_tier_price", precision = 12, scale = 2)
    private BigDecimal currentTierPrice;

    @Column(name = "committed_qty", nullable = false)
    @Builder.Default
    private Integer committedQty = 0;

    @Column(name = "target_qty", nullable = false)
    private Integer targetQty;

    @Column(name = "current_participants", nullable = false)
    @Builder.Default
    private Integer currentParticipants = 0;

    @Column(name = "target_participants")
    private Integer targetParticipants;

    @Column(name = "inventory_remaining")
    private Integer inventoryRemaining;

    @Column(name = "moq_label", length = 100)
    private String moqLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "deal_status", nullable = false)
    @Builder.Default
    private DealStatus dealStatus = DealStatus.OPEN;

    @Column(name = "deal_ends_at", nullable = false)
    private LocalDateTime dealEndsAt;

    @Column(name = "price_locked_at")
    private LocalDateTime priceLockedAt;

    @Column(name = "pickup_point", length = 150)
    private String pickupPoint;

    @Column(name = "pickup_date")
    private LocalDateTime pickupDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_type")
    private FulfillmentType fulfillmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;

    @Column(name = "is_trending")
    private Boolean isTrending;

    @Column(name = "is_almost_unlocked")
    private Boolean isAlmostUnlocked;

    @Column(name = "is_festival_deal")
    private Boolean isFestivalDeal;

    @OneToMany(mappedBy = "deal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("minQty ASC")
    @Builder.Default
    private List<GroupDealTier> priceTiers = new ArrayList<>();
}
