package com.manacommunity.api.devices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "smart_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "device_name", nullable = false, length = 200)
    private String deviceName;

    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(length = 100)
    private String vendor;

    @Column(length = 100)
    private String model;

    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    @Column(name = "api_key_ref", length = 200)
    private String apiKeyRef;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "is_online", nullable = false)
    private boolean online = true;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "accessible_by", nullable = false, length = 50)
    private String accessibleBy = "FM_ONLY";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
