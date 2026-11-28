package com.manacommunity.api.iot.edge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "device_heartbeat_logs", indexes = {
    @Index(name = "idx_heartbeat_device", columnList = "device_id"),
    @Index(name = "idx_heartbeat_time", columnList = "received_at")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DeviceHeartbeatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private Instant receivedAt = Instant.now();

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "cpu_usage_pct")
    private Integer cpuUsagePct;

    @Column(name = "memory_usage_pct")
    private Integer memoryUsagePct;

    @Column(name = "uptime_seconds")
    private Long uptimeSeconds;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "signal_strength")
    private Integer signalStrength;
}
