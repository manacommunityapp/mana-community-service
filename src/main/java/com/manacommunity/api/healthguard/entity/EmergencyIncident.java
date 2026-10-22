package com.manacommunity.api.healthguard.entity;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.AlertTriggerType;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.IncidentSeverity;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.IncidentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "emergency_incident", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String incidentUuid;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private IncidentSeverity severity = IncidentSeverity.LEVEL_3_POSSIBLE_EMERGENCY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.ACTIVE;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(length = 300)
    private String locationAddress;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private AlertTriggerType triggerType = AlertTriggerType.MANUAL_SOS;

    private LocalDateTime triggeredAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "text")
    private String notes;

    @Builder.Default
    private Integer alertsSent = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (incidentUuid == null) incidentUuid = UUID.randomUUID().toString();
        if (triggeredAt == null) triggeredAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
