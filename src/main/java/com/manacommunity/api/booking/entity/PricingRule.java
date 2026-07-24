package com.manacommunity.api.booking.entity;

import com.manacommunity.api.booking.entity.enums.PricingType;
import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "pricing_rule", indexes = {
        @Index(name = "idx_pricing_rule_resource", columnList = "resource_id"),
        @Index(name = "idx_pricing_rule_category", columnList = "category_id"),
        @Index(name = "idx_pricing_rule_community", columnList = "community_id"),
        @Index(name = "idx_pricing_rule_type", columnList = "pricing_type"),
        @Index(name = "idx_pricing_rule_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ResourceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 25)
    private PricingType pricingType;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "day_of_week", length = 50)
    private String dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
