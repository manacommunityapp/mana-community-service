package com.manacommunity.api.marketplace.cluster.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_cluster_listing", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketClusterListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_user_id", nullable = false)
    private AppUser seller;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, length = 40)
    private String category; // FURNITURE, CARPOOL, HOME_FOOD, ELECTRONICS, GENERAL

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Column(name = "visibility_scope", nullable = false, length = 20)
    private String visibilityScope = "CLUSTER_3KM"; // INTRA_SOCIETY, CLUSTER_3KM

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "s2_cell_id_l13", nullable = false, length = 16)
    private String s2CellIdL13;

    @Builder.Default
    @Column(name = "seller_trust_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal sellerTrustScore = new BigDecimal("1.00");

    @Builder.Default
    @Column(name = "is_seller_verified_resident", nullable = false)
    private Boolean isSellerVerifiedResident = true;

    @Builder.Default
    @Column(name = "moderation_status", nullable = false, length = 20)
    private String moderationStatus = "ACTIVE"; // ACTIVE, QUARANTINED, REJECTED

    @Builder.Default
    @Column(name = "fraud_risk_score", precision = 3, scale = 2)
    private BigDecimal fraudRiskScore = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
