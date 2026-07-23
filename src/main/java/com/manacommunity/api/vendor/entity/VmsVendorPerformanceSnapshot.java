package com.manacommunity.api.vendor.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vms_vendor_performance_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsVendorPerformanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VmsVendor vendor;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "period_type", nullable = false, length = 20)
    private String periodType;

    @Column(name = "bookings_count", nullable = false)
    @Builder.Default
    private Integer bookingsCount = 0;

    @Column(name = "revenue", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "completion_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal completionRate = BigDecimal.ZERO;

    @Column(name = "on_time_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal onTimeRate = BigDecimal.ZERO;

    @Column(name = "new_customers", nullable = false)
    @Builder.Default
    private Integer newCustomers = 0;

    @Column(name = "repeat_customers", nullable = false)
    @Builder.Default
    private Integer repeatCustomers = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
