package com.manacommunity.api.healthguard.entity;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.WearableType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wearable_device", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_identifier"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WearableDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WearableType deviceType = WearableType.GENERIC_BLUETOOTH;

    @Column(nullable = false, length = 120)
    private String deviceIdentifier;

    @Column(length = 80)
    private String deviceName;

    @Column(length = 20)
    private String firmwareVersion;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime lastSyncAt;

    @Column(updatable = false)
    private LocalDateTime registeredAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
