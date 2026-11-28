package com.manacommunity.api.opsresilience.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "incident_postmortems")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IncidentPostmortem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_code", nullable = false, unique = true, length = 64)
    private String incidentCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 16)
    private String severity; // SEV1, SEV2, SEV3

    @Builder.Default
    @Column(name = "blast_radius_pct", nullable = false)
    private double blastRadiusPct = 0.0;

    @Column(name = "root_cause_system", nullable = false, length = 128)
    private String rootCauseSystem;

    @Column(name = "mitigation_summary", columnDefinition = "TEXT")
    private String mitigationSummary;

    @Builder.Default
    @Column(nullable = false, length = 24)
    private String status = "OPEN"; // OPEN, MITIGATED, CLOSED

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
