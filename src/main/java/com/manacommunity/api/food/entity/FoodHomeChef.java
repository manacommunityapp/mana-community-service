package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_home_chefs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodHomeChef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "kitchen_name", nullable = false, length = 200)
    private String kitchenName;

    @Column(length = 2000)
    private String description;

    @Column(length = 200)
    private String speciality;

    @Column(name = "cuisine_types", columnDefinition = "TEXT")
    private String cuisineTypes;

    @Column(name = "fssai_license", length = 50)
    private String fssaiLicense;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChefStatus status = ChefStatus.PENDING;

    @Column(name = "verification_status", length = 50)
    private String verificationStatus;

    @Column(name = "max_orders_per_day")
    private Integer maxOrdersPerDay;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "total_ratings")
    @Builder.Default
    private Integer totalRatings = 0;

    @Column(name = "total_orders")
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "revenue_total", precision = 12, scale = 2)
    private BigDecimal revenueTotal;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", length = 20)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ChefStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum ChefStatus { PENDING, APPROVED, SUSPENDED }
    public enum AvailabilityStatus { AVAILABLE, BUSY, OFFLINE }
}
