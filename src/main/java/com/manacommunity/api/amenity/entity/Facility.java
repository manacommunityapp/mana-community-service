package com.manacommunity.api.amenity.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "facilities", uniqueConstraints = {
    @UniqueConstraint(name = "uq_facility_code", columnNames = {"community_id", "facility_code"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Facility extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "facility_code", nullable = false, length = 40)
    private String facilityCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "facility_type", nullable = false, length = 40)
    private String facilityType;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "slot_duration_minutes", nullable = false)
    @Builder.Default
    private Integer slotDurationMinutes = 60;

    @Column(name = "open_hour", nullable = false)
    @Builder.Default
    private Integer openHour = 6;

    @Column(name = "close_hour", nullable = false)
    @Builder.Default
    private Integer closeHour = 22;

    @Column(name = "peak_start_hour")
    @Builder.Default
    private Integer peakStartHour = 6;

    @Column(name = "peak_end_hour")
    @Builder.Default
    private Integer peakEndHour = 10;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(name = "max_bookings_per_flat_per_week", nullable = false)
    @Builder.Default
    private Integer maxBookingsPerFlatPerWeek = 3;

    @Column(name = "max_concurrent_peak_bookings", nullable = false)
    @Builder.Default
    private Integer maxConcurrentPeakBookings = 1;

    @Column(name = "advance_booking_days", nullable = false)
    @Builder.Default
    private Integer advanceBookingDays = 7;

    @Column(name = "cancellation_cutoff_hours", nullable = false)
    @Builder.Default
    private Integer cancellationCutoffHours = 2;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
