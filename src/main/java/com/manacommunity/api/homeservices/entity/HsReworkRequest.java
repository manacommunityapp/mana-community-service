package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "hs_rework_request") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsReworkRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "booking_id", nullable = false) private Long bookingId;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String reason;
    @Column(nullable = false, columnDefinition = "hs_rework_status") private String status = "REQUESTED";
    @Column(name = "rescheduled_at") private Instant rescheduledAt;
    @Column(name = "resolved_at") private Instant resolvedAt;
    private String notes;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
