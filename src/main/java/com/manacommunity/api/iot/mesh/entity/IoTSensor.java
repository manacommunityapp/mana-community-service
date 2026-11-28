package com.manacommunity.api.iot.mesh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "iot_sensors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IoTSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "sensor_name", nullable = false, length = 200)
    private String sensorName;

    @Column(name = "sensor_type", nullable = false, length = 50)
    private String sensorType;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(length = 20)
    private String unit;

    @Column(length = 100)
    private String vendor;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    @Column(name = "api_key_ref", length = 200)
    private String apiKeyRef;

    @Builder.Default
    @Column(name = "polling_interval_seconds", nullable = false)
    private int pollingIntervalSeconds = 300;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_reading", precision = 15, scale = 4)
    private BigDecimal lastReading;

    @Column(name = "last_reading_at")
    private Instant lastReadingAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
