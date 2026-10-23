package com.manacommunity.api.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "karate_batch")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KarateBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private KarateProgram program;

    @Column(nullable = false, length = 100)
    private String name;           // e.g. "Morning Batch — Jan 2026"

    @Column(nullable = false, length = 20)
    private String status;         // UPCOMING, ACTIVE, COMPLETED, CANCELLED

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "class_time")
    private LocalTime classTime;

    @Column(name = "days_of_week", length = 50)
    private String daysOfWeek;     // comma-separated: MON,WED,FRI

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "enrolled_count", nullable = false)
    private Integer enrolledCount = 0;

    @Column(name = "venue", length = 200)
    private String venue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "UPCOMING";
        if (enrolledCount == null) enrolledCount = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
