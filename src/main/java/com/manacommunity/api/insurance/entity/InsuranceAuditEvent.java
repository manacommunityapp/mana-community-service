package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_audit_event")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InsuranceAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_user_id")
    private Long actorUserId;
    @Column(nullable = false, length = 60)
    private String action;
    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;
    @Column(name = "entity_id", length = 40)
    private String entityId;
    @Column(length = 500)
    private String detail;
    @Column(name = "prev_hash", length = 64)
    private String prevHash;
    @Column(nullable = false, length = 64)
    private String hash;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
