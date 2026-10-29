package com.manacommunity.api.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_batch", indexes = {
    @Index(name = "idx_sports_batch_venue", columnList = "venue_id, is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private SportsCoach coach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    @JsonIgnore
    private SportsVenue venue;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 50)
    private String sport;

    @Column(name = "skill_level", nullable = false, length = 50)
    private String skillLevel;

    @Column(name = "age_group", nullable = false, length = 50)
    private String ageGroup;

    @Column(name = "schedule_days", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String scheduleDays = "[]";

    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime;

    @Column(name = "max_capacity", nullable = false)
    @Builder.Default
    private Integer maxCapacity = 15;

    @Column(name = "current_enrolled", nullable = false)
    @Builder.Default
    private Integer currentEnrolled = 0;

    @Column(name = "monthly_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monthlyFee = BigDecimal.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
