package com.manacommunity.api.marketplace.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_request_offers", indexes = {
    @Index(name = "idx_mkt_reqoff_req", columnList = "request_id"),
    @Index(name = "idx_mkt_reqoff_seller", columnList = "seller_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketRequestOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private MarketProductRequest productRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private AppUser seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private MarketListing listing;

    @Column(name = "offered_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal offeredPrice;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OfferStatus status = OfferStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = OfferStatus.PENDING;
    }

    public enum OfferStatus { PENDING, ACCEPTED, DECLINED }
}
