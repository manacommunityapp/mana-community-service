package com.manacommunity.api.federationrisk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "federation_sub_societies")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FederationSubSociety {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "federation_id", nullable = false)
    private Long federationId;

    @Column(name = "society_id", nullable = false, unique = true)
    private Long societyId;

    @Column(name = "phase_label", nullable = false, length = 64)
    private String phaseLabel;

    @Builder.Default
    @Column(name = "outer_gate_enabled", nullable = false)
    private boolean outerGateEnabled = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
