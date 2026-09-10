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
@Table(name = "marketplace_group_orders", indexes = {
    @Index(name = "idx_mkt_grp_comm", columnList = "community_id"),
    @Index(name = "idx_mkt_grp_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketGroupOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "target_quantity", nullable = false)
    private Integer targetQuantity;

    @Column(name = "current_quantity", nullable = false)
    @Builder.Default
    private Integer currentQuantity = 0;

    @Column(name = "current_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GroupOrderStatus status = GroupOrderStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private AppUser organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @OneToMany(mappedBy = "groupOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("minQuantity ASC")
    @Builder.Default
    private List<MarketGroupOrderTier> discountTiers = new ArrayList<>();

    @OneToMany(mappedBy = "groupOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MarketGroupOrderParticipant> participants = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentQuantity == null) currentQuantity = 0;
        if (currentPrice == null) currentPrice = basePrice;
        if (status == null) status = GroupOrderStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum GroupOrderStatus { ACTIVE, LOCKED, SUCCESSFUL, CANCELLED, FULFILLED }
}
