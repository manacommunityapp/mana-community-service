package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_subscription_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodSubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", length = 20)
    private PlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", length = 20)
    private TargetAudience targetAudience;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", length = 30)
    private ProviderType providerType;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "price_per_meal", precision = 10, scale = 2)
    private BigDecimal pricePerMeal;

    @Column(name = "monthly_price", precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "min_days")
    private Integer minDays;

    @Column(name = "includes_weekends")
    @Builder.Default
    private Boolean includesWeekends = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "nutrition_info", columnDefinition = "TEXT")
    private String nutritionInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum PlanType { BREAKFAST, LUNCH, DINNER, FULL_DAY }

    public enum TargetAudience { GENERAL, KIDS, GYM, DIABETIC, PREGNANCY, SENIOR, OFFICE, DIET }

    public enum ProviderType { RESTAURANT, HOME_CHEF, CLOUD_KITCHEN, COMMUNITY_KITCHEN }
}
