package com.manacommunity.api.events.entity;

import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
    name = "pooja_schedule",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_pooja_schedule_slot",
        columnNames = {"pooja_id", "schedule_date", "start_time"}
    ),
    indexes = {
        @Index(name = "idx_ps_pooja_date", columnList = "pooja_id, schedule_date"),
        @Index(name = "idx_ps_status",     columnList = "status")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoojaSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pooja_id", nullable = false)
    private PoojaSeva poojaSeva;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "family_capacity", nullable = false)
    @Builder.Default
    private Integer familyCapacity = 10;

    @Column(name = "devotee_capacity", nullable = false)
    @Builder.Default
    private Integer devoteeCapacity = 30;

    /**
     * Persisted admin state: OPEN or BLOCKED or CLOSED.
     * LIMITED / FULL are computed at query time from live availability.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PoojaScheduleStatus status = PoojaScheduleStatus.OPEN;

    /** Monotonic counter for Sankalpam token generation — incremented under the same lock. */
    @Column(name = "next_token_seq", nullable = false)
    @Builder.Default
    private Integer nextTokenSeq = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
