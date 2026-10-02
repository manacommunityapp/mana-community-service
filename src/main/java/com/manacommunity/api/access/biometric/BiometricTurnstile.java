package com.manacommunity.api.access.biometric;

import com.manacommunity.api.access.biometric.BiometricEnums.*;
import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_turnstiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricTurnstile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turnstile_identifier", nullable = false, unique = true, length = 100)
    private String turnstileIdentifier;

    @Column(name = "turnstile_name", nullable = false, length = 150)
    private String turnstileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "gate_location", nullable = false, length = 150)
    private String gateLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TurnstileDirection direction = TurnstileDirection.BIDIRECTIONAL;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "rtsp_stream_url", length = 255)
    private String rtspStreamUrl;

    @Builder.Default
    @Column(name = "relay_unlock_ms", nullable = false)
    private Integer relayUnlockMs = 3000;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private TurnstileStatus status = TurnstileStatus.ONLINE;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
