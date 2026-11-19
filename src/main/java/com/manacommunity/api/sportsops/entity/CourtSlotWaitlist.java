package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "court_slot_waitlist")
@Data
public class CourtSlotWaitlist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "booking_id", nullable = false) private Long bookingId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column private Integer position;
    @Column private Boolean notified = false;
    @Column(name = "notified_at") private Instant notifiedAt;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
