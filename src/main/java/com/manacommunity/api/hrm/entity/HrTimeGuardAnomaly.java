package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_timeguard_anomaly")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrTimeGuardAnomaly {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "anomaly_type", nullable = false, length = 50)
    private String anomalyType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "person_name", length = 120)
    private String personName;

    @Column(name = "person_role", length = 60)
    private String personRole;

    @Column(name = "agency_name", length = 150)
    private String agencyName;

    @Column(name = "gate_location", nullable = false, length = 100)
    private String gateLocation;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "forensic_details", columnDefinition = "TEXT")
    private String forensicDetails;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "FLAGGED";

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken;

    @Column(name = "event_timestamp")
    @Builder.Default
    private LocalDateTime eventTimestamp = LocalDateTime.now();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
