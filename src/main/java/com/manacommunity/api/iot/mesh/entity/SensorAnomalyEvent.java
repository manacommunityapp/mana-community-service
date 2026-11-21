package com.manacommunity.api.iot.mesh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sensor_anomaly_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SensorAnomalyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false)
    private Long sensorId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "alert_config_id")
    private Long alertConfigId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "detected_value", precision = 15, scale = 4)
    private BigDecimal detectedValue;

    @Column(name = "threshold_value", precision = 15, scale = 4)
    private BigDecimal thresholdValue;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt = Instant.now();
}
