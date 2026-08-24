package com.manacommunity.api.events.entity;

import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
    name = "event_pooja_schedule",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_pooja_schedule_slot",
        columnNames = {"pooja_id", "schedule_date", "start_time"}
    )
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPoojaSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pooja_id", nullable = false)
    private EventPoojaSeva poojaSeva;

    @Column(name = "community_id")
    private Long communityId;

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

    @Column(name = "notes", length = 500)
    private String notes;

    /** Running sequence for Sankalpam token numbers within this slot (1-based). */
    @Column(name = "next_token_seq", nullable = false)
    @Builder.Default
    private Integer nextTokenSeq = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
