package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "turnstiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Turnstile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "turnstile_code", nullable = false, unique = true, length = 50)
    private String turnstileCode;

    @Column(name = "turnstile_name", length = 150)
    private String turnstileName;

    @Column(name = "gate_location", length = 200)
    private String gateLocation;

    @Column(name = "turnstile_type", nullable = false, length = 30)
    private String turnstileType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "relay_pin")
    private Integer relayPin;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "confidence_threshold")
    private Double confidenceThreshold;

    @Column(name = "unlock_duration_seconds")
    private Integer unlockDurationSeconds;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "ONLINE";
        if (confidenceThreshold == null) confidenceThreshold = 0.80;
        if (unlockDurationSeconds == null) unlockDurationSeconds = 3;
    }
}
