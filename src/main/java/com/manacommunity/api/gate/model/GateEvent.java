package com.manacommunity.api.gate.model;

import com.manacommunity.api.gate.adapter.BarrierAction;
import com.manacommunity.api.gate.adapter.HardwareType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Hardware-agnostic audit log entry for every gate access event.
 * Written by GateHardwareDispatcher after each adapter decision.
 */
@Entity
@Table(name = "gate_event", indexes = {
        @Index(name = "idx_gate_event_community", columnList = "community_id"),
        @Index(name = "idx_gate_event_gate_id",   columnList = "gate_id"),
        @Index(name = "idx_gate_event_created_at",columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GateEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "gate_id", nullable = false, length = 50)
    private String gateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hardware_type", nullable = false, length = 20)
    private HardwareType hardwareType;

    /** ENTRY | EXIT */
    @Column(name = "direction", length = 10)
    private String direction;

    /** PLATE | RFID_TAG | QR_TOKEN | PIN | MANUAL */
    @Column(name = "identifier_type", length = 20)
    private String identifierType;

    @Column(name = "identifier_value", length = 100)
    private String identifierValue;

    /** RESIDENT | VISITOR | UNKNOWN */
    @Column(name = "matched_entity_type", length = 20)
    private String matchedEntityType;

    @Column(name = "matched_entity_id")
    private Long matchedEntityId;

    @Column(name = "matched_name", length = 100)
    private String matchedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "barrier_action", nullable = false, length = 10)
    private BarrierAction barrierAction;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
