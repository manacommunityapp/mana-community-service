package com.manacommunity.api.emergency.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sos_incident",
       indexes = {
           @Index(name = "idx_sos_community", columnList = "community_id"),
           @Index(name = "idx_sos_status", columnList = "status"),
           @Index(name = "idx_sos_triggered_at", columnList = "triggered_at")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SosIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private AppUser resident;

    @Column(name = "flat_number", length = 30)
    private String flatNumber;

    @Column(name = "building_block", length = 50)
    private String buildingBlock;

    @Enumerated(EnumType.STRING)
    @Column(name = "emergency_type", nullable = false, length = 30)
    @Builder.Default
    private EmergencyType emergencyType = EmergencyType.GENERAL_PANIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Severity severity = Severity.CRITICAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.TRIGGERED;

    @Column(precision = 10)
    private Double latitude;

    @Column(precision = 10)
    private Double longitude;

    @Column(name = "location_details", length = 200)
    private String locationDetails;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "lockdown_initiated", nullable = false)
    @Builder.Default
    private boolean lockdownInitiated = false;

    @Column(name = "sla_target_seconds", nullable = false)
    @Builder.Default
    private int slaTargetSeconds = 180; // 3 minutes default

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private AppUser resolvedBy;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SosDispatch> dispatches = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (triggeredAt == null) triggeredAt = LocalDateTime.now();
    }

    public enum EmergencyType {
        MEDICAL, FIRE, SECURITY_INTRUDER, GAS_LEAK, LIFT_STUCK, GENERAL_PANIC, THEFT
    }

    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW }

    public enum IncidentStatus {
        TRIGGERED, ACKNOWLEDGED, DISPATCHED, ON_SITE, RESOLVED, FALSE_ALARM
    }
}