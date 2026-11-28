package com.manacommunity.api.edgehardware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "edge_gateway_ota_state")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EdgeGatewayOtaState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "controller_id", nullable = false, unique = true, length = 64)
    private String controllerId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Builder.Default
    @Column(name = "active_slot", nullable = false, length = 4)
    private String activeSlot = "A";

    @Builder.Default
    @Column(name = "target_slot", nullable = false, length = 4)
    private String targetSlot = "A";

    @Column(name = "current_version", nullable = false, length = 64)
    private String currentVersion;

    @Column(name = "pending_version", length = 64)
    private String pendingVersion;

    @Builder.Default
    @Column(name = "upgrade_available", nullable = false)
    private boolean upgradeAvailable = false;

    @Builder.Default
    @Column(name = "boot_count", nullable = false)
    private int bootCount = 0;

    @Builder.Default
    @Column(name = "boot_limit", nullable = false)
    private int bootLimit = 1;

    @Builder.Default
    @Column(name = "soc_temp_celsius", nullable = false)
    private double socTempCelsius = 45.0;

    @Builder.Default
    @Column(name = "ota_status", nullable = false, length = 32)
    private String otaStatus = "HEALTHY";

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
