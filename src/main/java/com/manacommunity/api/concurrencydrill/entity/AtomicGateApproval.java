package com.manacommunity.api.concurrencydrill.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "atomic_gate_approvals")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AtomicGateApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "visitor_name", nullable = false, length = 128)
    private String visitorName;

    @Builder.Default
    @Column(nullable = false, length = 24)
    private String status = "PENDING"; // PENDING, APPROVED, DENIED

    @Builder.Default
    @Column(nullable = false)
    private int version = 1;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
