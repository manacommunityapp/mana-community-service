package com.manacommunity.api.patrol.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patrol_breach_incident", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatrolBreachIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkpoint_id", nullable = false)
    private PatrolCheckpointNode checkpoint;

    @Column(name = "guard_user_id")
    private Long guardUserId;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "expected_deadline", nullable = false)
    private LocalDateTime expectedDeadline;

    @Column(name = "breach_type", nullable = false, length = 30)
    private String breachType; // MISSED_CHECKPOINT, LATE_SCAN, SPOOF_ATTEMPT

    @Builder.Default
    @Column(name = "escalation_status", nullable = false, length = 20)
    private String escalationStatus = "OPEN"; // OPEN, SUPERVISOR_ACK, RESOLVED

    @Builder.Default
    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt = LocalDateTime.now();
}
