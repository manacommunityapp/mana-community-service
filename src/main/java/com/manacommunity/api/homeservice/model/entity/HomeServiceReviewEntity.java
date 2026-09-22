package com.manacommunity.api.homeservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "home_service_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeServiceReviewEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "booking_id", length = 64)
    private String bookingId;

    @Column(name = "reviewer_user_id", nullable = false, length = 64)
    private String reviewerUserId;

    @Column(name = "reviewer_name", length = 150)
    private String reviewerName;

    @Column(name = "reviewer_flat_info", length = 100)
    private String reviewerFlatInfo;

    @Column(name = "reviewee_worker_id", nullable = false, length = 64)
    private String revieweeWorkerId;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "work_quality")
    @Builder.Default
    private Integer workQuality = 5;

    @Column(name = "punctuality")
    @Builder.Default
    private Integer punctuality = 5;

    @Column(name = "behaviour")
    @Builder.Default
    private Integer behaviour = 5;

    @Column(name = "reliability")
    @Builder.Default
    private Integer reliability = 5;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "is_verified_resident")
    @Builder.Default
    private Boolean verifiedResident = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
