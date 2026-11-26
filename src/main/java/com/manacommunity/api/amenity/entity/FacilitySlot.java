package com.manacommunity.api.amenity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "facility_slots", uniqueConstraints = {
    @UniqueConstraint(name = "uq_slot", columnNames = {"facility_id", "slot_date", "start_time"})
}, indexes = {
    @Index(name = "idx_slot_lookup", columnList = "facility_id, slot_date, status")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FacilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "facility_id", nullable = false)
    private Long facilityId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "booked_by_resident_id")
    private Long bookedByResidentId;

    @Column(name = "booked_by_flat_no", length = 20)
    private String bookedByFlatNo;

    @Column(name = "booking_reference", length = 40)
    private String bookingReference;

    @Column(name = "is_peak_hour", nullable = false)
    @Builder.Default
    private Boolean isPeakHour = false;

    @Version
    private Long version;
}
