package com.manacommunity.api.safety.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "guard_shift", indexes = {
        @Index(name = "idx_guard_shift_guard", columnList = "guard_id"),
        @Index(name = "idx_guard_shift_community", columnList = "community_id"),
        @Index(name = "idx_guard_shift_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guard_id", nullable = false)
    private AppUser guard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(length = 100)
    private String area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShiftStatus status = ShiftStatus.ON_DUTY;

    @Builder.Default
    @Column(name = "checkpoints_completed", nullable = false)
    private Integer checkpointsCompleted = 0;

    @Builder.Default
    @Column(name = "total_checkpoints", nullable = false)
    private Integer totalCheckpoints = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ShiftStatus {
        ON_DUTY, OFF_DUTY, BREAK
    }
}
