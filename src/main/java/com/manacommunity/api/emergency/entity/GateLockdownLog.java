package com.manacommunity.api.emergency.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gate_lockdown_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateLockdownLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private SosIncident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "lockdown_directive", nullable = false, length = 30)
    private LockdownDirective directive;

    @Column(name = "affected_gates", length = 200)
    @Builder.Default
    private String affectedGates = "ALL_GATES";

    @Column(name = "reason", length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by_id")
    private AppUser initiatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum LockdownDirective {
        LOCKDOWN_CLOSE_ALL,  // Close and lock all barriers (Intruder, Robbery)
        EVACUATION_OPEN_ALL, // Open and hold all barriers (Fire, Earthquake)
        NORMAL_RESTORE       // Restore automated ANPR barrier operation
    }
}