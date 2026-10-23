package com.manacommunity.api.safety.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patrol_log", indexes = {
        @Index(name = "idx_patrol_log_guard", columnList = "guard_id"),
        @Index(name = "idx_patrol_log_checkpoint", columnList = "checkpoint_id"),
        @Index(name = "idx_patrol_log_shift", columnList = "shift_id"),
        @Index(name = "idx_patrol_log_community", columnList = "community_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatrolLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guard_id", nullable = false)
    private AppUser guard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkpoint_id", nullable = false)
    private PatrolCheckpoint checkpoint;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
