package com.manacommunity.api.patrol.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "patrol_checkpoint_node", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(name = "uq_checkpoint_route_seq", columnNames = {"community_id", "route_name", "sequence_order"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatrolCheckpointNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "route_name", nullable = false, length = 100)
    private String routeName;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Builder.Default
    @Column(name = "geofence_radius_meters", nullable = false)
    private Integer geofenceRadiusMeters = 25;

    @Builder.Default
    @Column(name = "tag_type", nullable = false, length = 20)
    private String tagType = "NTAG_424_DNA"; // NTAG_424_DNA, TOTP_QR, BLE_BEACON

    @Column(name = "nfc_tag_uid", length = 32)
    private String nfcTagUid;

    @Column(name = "nfc_key_secret", length = 64)
    private String nfcKeySecret;

    @Builder.Default
    @Column(name = "last_known_tap_counter", nullable = false)
    private Long lastKnownTapCounter = 0L;

    @Builder.Default
    @Column(name = "expected_offset_minutes", nullable = false)
    private Integer expectedOffsetMinutes = 0;

    @Builder.Default
    @Column(name = "allowed_grace_minutes", nullable = false)
    private Integer allowedGraceMinutes = 10;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
