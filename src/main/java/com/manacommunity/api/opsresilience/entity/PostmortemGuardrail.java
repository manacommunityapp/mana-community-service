package com.manacommunity.api.opsresilience.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "postmortem_guardrails")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PostmortemGuardrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "postmortem_id", nullable = false)
    private Long postmortemId;

    @Column(name = "guardrail_type", nullable = false, length = 32)
    private String guardrailType; // BULKHEAD_POOL, RUSH_HOUR_FREEZE, AUTO_ROLLBACK, CHAOS_GAMEDAY

    @Column(nullable = false, length = 512)
    private String description;

    @Column(name = "owner_team", nullable = false, length = 64)
    private String ownerTeam;

    @Builder.Default
    @Column(nullable = false, length = 24)
    private String status = "PENDING"; // PENDING, IMPLEMENTED, VERIFIED

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
