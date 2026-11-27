package com.manacommunity.api.amenity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "flat_booking_quotas", uniqueConstraints = {
    @UniqueConstraint(name = "uq_flat_quota", columnNames = {"community_id", "flat_no", "facility_id", "week_start"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FlatBookingQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "flat_no", nullable = false, length = 20)
    private String flatNo;

    @Column(name = "facility_id", nullable = false)
    private Long facilityId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "bookings_used", nullable = false)
    @Builder.Default
    private Integer bookingsUsed = 0;

    @Column(name = "active_peak_bookings", nullable = false)
    @Builder.Default
    private Integer activePeakBookings = 0;

    @Version
    private Long version;
}
