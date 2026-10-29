package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Immutable (DB trigger blocks UPDATE/DELETE). Unique (policy_id, event_id) makes partner events idempotent. */
@Entity
@Table(name = "insurance_policy_status_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PolicyStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    @Column(name = "from_status", length = 20)
    private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;
    @Column(nullable = false, length = 20)
    private String source;
    @Column(name = "event_id", length = 80)
    private String eventId;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
