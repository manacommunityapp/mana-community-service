package com.manacommunity.api.sports.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "trainer_slot", schema = "sports",
        indexes = {
                @Index(name = "idx_trainer_slot_trainer", columnList = "trainer_id"),
                @Index(name = "idx_trainer_slot_date", columnList = "slot_date"),
                @Index(name = "idx_trainer_slot_sport", columnList = "sport"),
                @Index(name = "idx_trainer_slot_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    private SportsTrainer trainer;

    @Column(nullable = false, length = 100)
    private String sport;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /** INDIVIDUAL or GROUP */
    @Column(name = "slot_type", nullable = false, length = 20)
    @Builder.Default
    private String slotType = "INDIVIDUAL";

    @Column(name = "max_capacity", nullable = false)
    @Builder.Default
    private Integer maxCapacity = 1;

    @Column(name = "booked_count", nullable = false)
    @Builder.Default
    private Integer bookedCount = 0;

    @Column(name = "venue_id")
    private Long venueId;

    @Column(name = "venue_name", length = 150)
    private String venueName;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    /** OPEN | BOOKED | CANCELLED | COMPLETED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";

    @Column(length = 500)
    private String notes;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
