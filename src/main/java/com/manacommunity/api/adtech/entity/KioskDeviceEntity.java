package com.manacommunity.api.adtech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "kiosk_devices", schema = "adtech")
@Getter
@Setter
@NoArgsConstructor
public class KioskDeviceEntity {

    @Id
    @Column(name = "kiosk_id", nullable = false, length = 100)
    private String kioskId;

    @Column(name = "society_id", nullable = false, length = 100)
    private String societyId;

    @Column(name = "label")
    private String label;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
