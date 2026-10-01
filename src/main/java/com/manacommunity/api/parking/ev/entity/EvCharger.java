package com.manacommunity.api.parking.ev.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.entity.ParkingSlot;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ev_charger")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvCharger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_slot_id", nullable = false)
    private ParkingSlot parkingSlot;

    @Column(name = "device_id", nullable = false, unique = true, length = 60)
    private String deviceId;

    @Column(name = "connector_type", nullable = false, length = 30)
    @Builder.Default
    private String connectorType = "TYPE2"; // TYPE2, CCS2, CHADEMO, GB/T

    @Column(name = "max_kw_rating", nullable = false)
    @Builder.Default
    private Double maxKwRating = 7.4; // 7.4 kW, 11 kW, 22 kW, 50 kW

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChargerStatus status = ChargerStatus.AVAILABLE;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ChargerStatus { AVAILABLE, CHARGING, FAULT, OFFLINE }
}