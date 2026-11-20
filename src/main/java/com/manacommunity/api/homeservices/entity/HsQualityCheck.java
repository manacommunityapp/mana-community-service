package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "hs_quality_check") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsQualityCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "booking_id", nullable = false, unique = true) private Long bookingId;
    @Column(name = "checked_by", nullable = false) private Long checkedBy;
    @Column(name = "overall_score", nullable = false) private Integer overallScore;
    private Integer cleanliness;
    private Integer timeliness;
    private Integer workmanship;
    private String notes;
    @Column(name = "checked_at") private Instant checkedAt = Instant.now();
}
