package com.manacommunity.api.edgehardware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "edge_barrier_command_nonces",
    uniqueConstraints = @UniqueConstraint(name = "uq_edge_controller_cmd_nonce", columnNames = {"controller_id", "cmd_nonce"})
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EdgeBarrierCommandNonce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "controller_id", nullable = false, length = 64)
    private String controllerId;

    @Column(name = "cmd_nonce", nullable = false, length = 128)
    private String cmdNonce;

    @Column(name = "command_type", nullable = false, length = 24)
    private String commandType;

    @Column(name = "loop_a_occupied", nullable = false)
    private boolean loopAOccupied;

    @Column(name = "loop_b_occupied", nullable = false)
    private boolean loopBOccupied;

    @Column(nullable = false, length = 40)
    private String outcome;

    @Builder.Default
    @Column(name = "tailgate_detected", nullable = false)
    private boolean tailgateDetected = false;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
