package com.manacommunity.api.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "workflow_transition_history")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowTransitionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "from_status", nullable = false)
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(name = "reason")
    private String reason;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "transitioned_at", nullable = false)
    private Instant transitionedAt;
}
