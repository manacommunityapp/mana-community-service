package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "hs_booking") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsBooking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "worker_id") private Long workerId;
    @Column(name = "package_id") private Long packageId;
    @Column(nullable = false, columnDefinition = "hs_service_category") private String category;
    @Column(name = "booking_type", nullable = false, columnDefinition = "hs_booking_type") private String bookingType = "SCHEDULED";
    @Column(name = "scheduled_at") private Instant scheduledAt;
    @Column(nullable = false, columnDefinition = "hs_booking_status") private String status = "PENDING";
    @Column(name = "flat_number") private String flatNumber;
    private String description;
    @Column(name = "quoted_price", precision = 12, scale = 2) private BigDecimal quotedPrice;
    @Column(name = "final_price", precision = 12, scale = 2) private BigDecimal finalPrice;
    private String notes;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
    @Column(name = "updated_at") private Instant updatedAt = Instant.now();
}
