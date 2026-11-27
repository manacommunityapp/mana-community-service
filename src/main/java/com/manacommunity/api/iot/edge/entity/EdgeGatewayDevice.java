package com.manacommunity.api.iot.edge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "edge_gateway_devices", uniqueConstraints = {
    @UniqueConstraint(name = "uq_edge_device_serial", columnNames = {"serial_number"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EdgeGatewayDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "serial_number", nullable = false, unique = true, length = 80)
    private String serialNumber;

    @Column(name = "device_type", nullable = false, length = 40)
    private String deviceType;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(length = 100)
    private String vendor;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private Boolean isOnline = true;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "heartbeat_interval_seconds", nullable = false)
    @Builder.Default
    private Integer heartbeatIntervalSeconds = 30;

    @Column(name = "consecutive_misses", nullable = false)
    @Builder.Default
    private Integer consecutiveMisses = 0;

    @Column(name = "offline_since")
    private Instant offlineSince;

    @Column(name = "region_tag", length = 60)
    private String regionTag;

    @Column(name = "isp_provider", length = 60)
    private String ispProvider;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
