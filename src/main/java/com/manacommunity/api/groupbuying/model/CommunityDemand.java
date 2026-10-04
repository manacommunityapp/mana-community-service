package com.manacommunity.api.groupbuying.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_buy_demand")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityDemand extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdByUser;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "interested_residents", nullable = false)
    @Builder.Default
    private Integer interestedResidents = 1;

    @Column(name = "expected_qty")
    private Integer expectedQty;

    @Column(name = "upvotes_count", nullable = false)
    @Builder.Default
    private Integer upvotesCount = 1;

    @Column(name = "target_upvotes")
    @Builder.Default
    private Integer targetUpvotes = 25;

    @Column(name = "preferred_price_min", precision = 12, scale = 2)
    private BigDecimal preferredPriceMin;

    @Column(name = "preferred_price_max", precision = 12, scale = 2)
    private BigDecimal preferredPriceMax;

    @Column(name = "preferred_brand", length = 100)
    private String preferredBrand;

    @Column(name = "preferred_pack_size", length = 50)
    private String preferredPackSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DemandStatus status = DemandStatus.OPEN;

    @OneToMany(mappedBy = "demand", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommunityDemandOffer> vendorOffers = new ArrayList<>();
}
