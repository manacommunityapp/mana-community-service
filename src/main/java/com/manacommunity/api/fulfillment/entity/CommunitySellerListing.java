package com.manacommunity.api.fulfillment.entity;

import com.manacommunity.api.marketplace.entity.MarketSellerProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "community_seller_listings")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommunitySellerListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private MarketSellerProfile seller;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "price_unit", length = 50)
    private String priceUnit;

    @JdbcTypeCode(SqlTypes.JSON)
    private Object images;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "slot_based", nullable = false)
    private Boolean slotBased = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
