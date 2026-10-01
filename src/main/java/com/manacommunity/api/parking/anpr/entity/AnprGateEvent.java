package com.manacommunity.api.parking.anpr.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit log for every plate recognition event received from the ANPR camera
 * (via the mana-community-anpr-services Python microservice webhook).
 *
 * barrier_action:
 *   OPEN  — plate matched to an ACTIVE resident vehicle or valid visitor pass
 *   HOLD  — plate unrecognised / low confidence — security guard notified
 *   DENY  — plate explicitly blacklisted
 */
@Entity
@Table(name = "anpr_gate_event",
       indexes = {
           @Index(name = "idx_anpr_gate_id", columnList = "gate_id"),
           @Index(name = "idx_anpr_plate", columnList = "plate_number"),
           @Index(name = "idx_anpr_created_at", columnList = "created_at")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnprGateEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    /** Gate identifier: GATE_MAIN_IN, GATE_MAIN_OUT, GATE_BASEMENT, etc. */
    @Column(name = "gate_id", nullable = false, length = 50)
    private String gateId;

    /** ENTRY or EXIT */
    @Column(name = "direction", nullable = false, length = 10)
    @Builder.Default
    private String direction = "ENTRY";

    /** Normalised plate number as recognised by ANPR (e.g. MH12AB1234). */
    @Column(name = "plate_number", length = 20)
    private String plateNumber;

    /** Raw OCR text before normalisation. */
    @Column(name = "raw_ocr_text", length = 50)
    private String rawOcrText;

    /** Combined detection+OCR confidence score (0.00–1.00). */
    @Column(name = "confidence")
    private Double confidence;

    /** STANDARD | BH_SERIES | TRADE | UNKNOWN */
    @Column(name = "plate_format", length = 20)
    private String plateFormat;

    /** OPEN | HOLD | DENY */
    @Enumerated(EnumType.STRING)
    @Column(name = "barrier_action", nullable = false, length = 10)
    @Builder.Default
    private BarrierAction barrierAction = BarrierAction.HOLD;

    /**
     * Recognition result from ANPR service.
     * SUCCESS | LOW_CONFIDENCE | INVALID_PLATE | NO_PLATE_FOUND
     */
    @Column(name = "anpr_status", length = 30)
    private String anprStatus;

    /** FK to ResidentVehicle.id — set when plate matched a registered vehicle. */
    @Column(name = "matched_vehicle_id")
    private Long matchedVehicleId;

    /** FK to ParkingVisitorPass.id — set when plate matched an active visitor pass. */
    @Column(name = "matched_visitor_pass_id")
    private Long matchedVisitorPassId;

    /** Resident name for display on security dashboard. */
    @Column(name = "matched_resident_name", length = 100)
    private String matchedResidentName;

    /** Processing time reported by the ANPR service in milliseconds. */
    @Column(name = "processing_ms")
    private Double processingMs;

    /** External event ID from mana-anpr-service (for cross-service correlation). */
    @Column(name = "external_event_id")
    private Long externalEventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum BarrierAction { OPEN, HOLD, DENY }
}
