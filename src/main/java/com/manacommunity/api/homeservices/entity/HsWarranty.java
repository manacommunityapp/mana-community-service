package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "hs_warranty") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsWarranty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "booking_id", nullable = false, unique = true) private Long bookingId;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "warranty_days", nullable = false) private Integer warrantyDays = 30;
    @Column(name = "expires_at", nullable = false) private LocalDate expiresAt;
    @Column(nullable = false, columnDefinition = "hs_warranty_status") private String status = "ACTIVE";
    @Column(name = "claim_reason") private String claimReason;
    @Column(name = "claimed_at") private Instant claimedAt;
}
