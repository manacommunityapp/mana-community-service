package com.manacommunity.api.gate.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Security alert raised whenever a gate event results in a HOLD decision
 * (unrecognised plate, unknown RFID tag, etc.).
 *
 * Guards see open alerts in real-time via STOMP; they acknowledge and resolve them.
 */
@Entity
@Table(name = "security_alert", indexes = {
        @Index(name = "idx_security_alert_community", columnList = "community_id"),
        @Index(name = "idx_security_alert_status",    columnList = "alert_status"),
        @Index(name = "idx_security_alert_created",   columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "gate_id", nullable = false, length = 50)
    private String gateId;

    @Column(name = "gate_event_id")
    private Long gateEventId;

    /** ANPR | RFID | SMART_LOCK | BOOM_BARRIER | QR_SCAN | MANUAL */
    @Column(name = "hardware_type", length = 20)
    private String hardwareType;

    /** PLATE | RFID_TAG | QR_TOKEN | PIN | MANUAL */
    @Column(name = "identifier_type", length = 20)
    private String identifierType;

    @Column(name = "identifier_value", length = 100)
    private String identifierValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_status", nullable = false, length = 20)
    @Builder.Default
    private AlertStatus alertStatus = AlertStatus.OPEN;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AlertStatus {
        OPEN,         // raised, no guard has looked at it
        ACKNOWLEDGED, // guard is handling it on-site
        RESOLVED      // closed with a resolution note
    }
}
