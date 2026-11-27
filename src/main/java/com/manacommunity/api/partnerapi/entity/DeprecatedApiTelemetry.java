package com.manacommunity.api.partnerapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "deprecated_api_telemetry",
    uniqueConstraints = @UniqueConstraint(name = "uq_deprecated_endpoint_serial", columnNames = {"endpoint", "controller_serial"})
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DeprecatedApiTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String endpoint;

    @Column(name = "api_version", nullable = false, length = 16)
    private String apiVersion;

    @Column(name = "controller_serial", nullable = false, length = 64)
    private String controllerSerial;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "firmware_version", length = 64)
    private String firmwareVersion;

    @Builder.Default
    @Column(name = "call_count", nullable = false)
    private long callCount = 1L;

    @Builder.Default
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();
}
