package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "home_service_booking", indexes = {
        @Index(name = "idx_home_svc_booking_community", columnList = "community_id"),
        @Index(name = "idx_home_svc_booking_resident", columnList = "resident_user_id"),
        @Index(name = "idx_home_svc_booking_provider", columnList = "provider_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeServiceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private HomeServiceProvider provider;

    @Column(name = "flat_no", length = 30)
    private String flatNo;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "REQUESTED";

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_status", length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING";

    @Column(name = "resident_rating")
    private Integer residentRating;

    @Column(name = "resident_review", columnDefinition = "TEXT")
    private String residentReview;

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
