package com.manacommunity.api.patrol.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "patrol_scan_audit", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatrolScanAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkpoint_id", nullable = false)
    private PatrolCheckpointNode checkpoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guard_user_id", nullable = false)
    private AppUser guardUser;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "scanned_at_device_boot_ms", nullable = false)
    private Long scannedAtDeviceBootMs;

    @Builder.Default
    @Column(name = "server_received_at", nullable = false)
    private LocalDateTime serverReceivedAt = LocalDateTime.now();

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "gps_accuracy_meters", precision = 6, scale = 2)
    private BigDecimal gpsAccuracyMeters;

    @Builder.Default
    @Column(name = "is_mock_detected", nullable = false)
    private Boolean isMockDetected = false;

    @Builder.Default
    @Column(name = "nfc_counter_verified", nullable = false)
    private Boolean nfcCounterVerified = true;

    @Builder.Default
    @Column(name = "ble_beacon_verified", nullable = false)
    private Boolean bleBeaconVerified = true;

    @Column(name = "kinematic_speed_kmh", precision = 5, scale = 2)
    private BigDecimal kinematicSpeedKmh;

    @Column(name = "scan_validity_status", nullable = false, length = 30)
    private String scanValidityStatus; // VALID, SPOOF_REJECTED, REPLAY_REJECTED, OUT_OF_GEOFENCE, KINEMATIC_TELEPORTATION_SPOOF

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;
}
