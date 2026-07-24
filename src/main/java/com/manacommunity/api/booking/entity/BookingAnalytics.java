package com.manacommunity.api.booking.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "booking_analytics", indexes = {
        @Index(name = "idx_booking_analytics_resource", columnList = "resource_id"),
        @Index(name = "idx_booking_analytics_date", columnList = "analytics_date"),
        @Index(name = "idx_booking_analytics_community", columnList = "community_id"),
        @Index(name = "idx_booking_analytics_resource_date", columnList = "resource_id, analytics_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "analytics_date", nullable = false)
    private LocalDate analyticsDate;

    @Column(name = "total_bookings")
    @Builder.Default
    private Integer totalBookings = 0;

    @Column(name = "confirmed_bookings")
    @Builder.Default
    private Integer confirmedBookings = 0;

    @Column(name = "cancelled_bookings")
    @Builder.Default
    private Integer cancelledBookings = 0;

    @Column(name = "no_shows")
    @Builder.Default
    private Integer noShows = 0;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "occupancy_percentage", precision = 5, scale = 2)
    private BigDecimal occupancyPercentage;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "peak_hour_start")
    private LocalTime peakHourStart;

    @Column(name = "peak_hour_end")
    private LocalTime peakHourEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
