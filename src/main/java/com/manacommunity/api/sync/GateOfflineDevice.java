package com.manacommunity.api.sync;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "gate_offline_device",
    uniqueConstraints = @UniqueConstraint(name = "uq_gate_offline_device", columnNames = {"community_id", "device_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateOfflineDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "gate_name", nullable = false, length = 100)
    private String gateName;

    @Column(name = "signing_secret", nullable = false, length = 255)
    private String signingSecret;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(name = "last_seq", nullable = false)
    private Long lastSeq = 0L;

    @Builder.Default
    @Column(name = "roster_version", nullable = false)
    private Long rosterVersion = 1L;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
