package com.manacommunity.api.vendor.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vms_vendor_performance", uniqueConstraints = @UniqueConstraint(columnNames = {"vendor_id", "community_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsVendorPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VmsVendor vendor;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "total_ratings", nullable = false)
    @Builder.Default
    private Integer totalRatings = 0;

    @Column(name = "total_bookings", nullable = false)
    @Builder.Default
    private Integer totalBookings = 0;

    @Column(name = "completed_bookings", nullable = false)
    @Builder.Default
    private Integer completedBookings = 0;

    @Column(name = "cancelled_bookings", nullable = false)
    @Builder.Default
    private Integer cancelledBookings = 0;

    @Column(name = "on_time_completion_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal onTimeCompletionRate = BigDecimal.ZERO;

    @Column(name = "response_time_minutes")
    @Builder.Default
    private Integer responseTimeMinutes = 0;

    @Column(name = "total_revenue", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "performance_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal performanceScore = BigDecimal.ZERO;

    @Column(name = "performance_tier", length = 20)
    @Builder.Default
    private String performanceTier = "BRONZE";

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
}
