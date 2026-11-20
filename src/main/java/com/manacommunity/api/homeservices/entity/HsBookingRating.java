package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "hs_booking_rating") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsBookingRating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "booking_id", nullable = false, unique = true) private Long bookingId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "worker_id", nullable = false) private Long workerId;
    @Column(nullable = false) private Integer overall;
    private Integer punctuality;
    private Integer quality;
    private String comment;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
