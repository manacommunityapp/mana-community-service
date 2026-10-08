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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTurnstileIdentifier() { return turnstileIdentifier; }
    public void setTurnstileIdentifier(String turnstileIdentifier) { this.turnstileIdentifier = turnstileIdentifier; }
    public String getTurnstileName() { return turnstileName; }
    public void setTurnstileName(String turnstileName) { this.turnstileName = turnstileName; }
    public Community getCommunity() { return community; }
    public void setCommunity(Community community) { this.community = community; }
    public String getGateLocation() { return gateLocation; }
    public void setGateLocation(String gateLocation) { this.gateLocation = gateLocation; }
    public TurnstileDirection getDirection() { return direction; }
    public void setDirection(TurnstileDirection direction) { this.direction = direction; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getRtspStreamUrl() { return rtspStreamUrl; }
    public void setRtspStreamUrl(String rtspStreamUrl) { this.rtspStreamUrl = rtspStreamUrl; }
    public Integer getRelayUnlockMs() { return relayUnlockMs; }
    public void setRelayUnlockMs(Integer relayUnlockMs) { this.relayUnlockMs = relayUnlockMs; }
    public TurnstileStatus getStatus() { return status; }
    public void setStatus(TurnstileStatus status) { this.status = status; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
