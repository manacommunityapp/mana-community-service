package com.manacommunity.api.amenity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "booking_waitlist", indexes = {
    @Index(name = "idx_waitlist_slot", columnList = "slot_id, position"),
    @Index(name = "idx_waitlist_resident", columnList = "resident_id")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingWaitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "facility_id", nullable = false)
    private Long facilityId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "flat_no", length = 20)
    private String flatNo;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "WAITING";

    @Column(name = "offered_at")
    private Instant offeredAt;

    @Column(name = "offer_expires_at")
    private Instant offerExpiresAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
