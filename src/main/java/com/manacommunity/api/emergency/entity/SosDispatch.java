package com.manacommunity.api.emergency.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sos_dispatch")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SosDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private SosIncident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guard_id", nullable = false)
    private AppUser guard;

    @Column(name = "dispatched_at", nullable = false)
    private LocalDateTime dispatchedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "response_duration_seconds")
    private Long responseDurationSeconds;

    @Column(name = "is_sla_breached", nullable = false)
    @Builder.Default
    private boolean slaBreached = false;

    @Column(name = "guard_notes", length = 500)
    private String guardNotes;

    @PrePersist
    void onCreate() {
        if (dispatchedAt == null) dispatchedAt = LocalDateTime.now();
    }
}