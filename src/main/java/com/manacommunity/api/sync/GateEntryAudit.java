package com.manacommunity.api.sync;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gate_entry_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateEntryAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "local_seq")
    private Long localSeq;

    @Column(name = "client_mutation_id", nullable = false, length = 100)
    private String clientMutationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private SyncEnums.SyncEntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "pass_code", length = 100)
    private String passCode;

    @Column(name = "pass_nonce", length = 128)
    private String passNonce;

    @Column(name = "gate_name", length = 100)
    private String gateName;

    @Column(name = "guard_user_id")
    private Long guardUserId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Builder.Default
    @Column(name = "reconciled_at", nullable = false)
    private LocalDateTime reconciledAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 30)
    private SyncEnums.SyncStatus syncStatus;

    @Builder.Default
    @Column(name = "offline_override", nullable = false)
    private boolean offlineOverride = false;

    @Column(name = "conflict_reason", length = 500)
    private String conflictReason;

    @Column(name = "device_signature", length = 128)
    private String deviceSignature;

    @Builder.Default
    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;
}
