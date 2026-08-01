package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_caterers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodCaterer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "cuisine_types", columnDefinition = "TEXT")
    private String cuisineTypes;

    @Column(name = "min_order_count")
    private Integer minOrderCount;

    @Column(name = "max_order_count")
    private Integer maxOrderCount;

    @Column(name = "price_per_plate_from", precision = 10, scale = 2)
    private BigDecimal pricePerPlateFrom;

    @Column(name = "price_per_plate_to", precision = 10, scale = 2)
    private BigDecimal pricePerPlateTo;

    @Column(name = "fssai_license", length = 50)
    private String fssaiLicense;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "total_events")
    @Builder.Default
    private Integer totalEvents = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CatererStatus status = CatererStatus.ACTIVE;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum CatererStatus { ACTIVE, INACTIVE, SUSPENDED }
}
