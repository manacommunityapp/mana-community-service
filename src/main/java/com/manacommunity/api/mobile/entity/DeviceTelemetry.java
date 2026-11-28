package com.manacommunity.api.mobile.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_telemetry")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceTelemetry extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String deviceId;
    private String oem;
    private String model;
    private String osVersion;
    private String appVersion;

    @Enumerated(EnumType.STRING)
    private DeviceRole role;

    private int batteryPct;
    private boolean batteryOptimizationExempt;
    private boolean autoStartEnabled;
    private String backgroundRestrictionLevel;
    private boolean fcmTokenValid;
    private String fcmDeliveryChannel;
    private long lastFcmLatencyMs;

    private long coldStartMs;
    private long warmStartMs;
    private long dexClassCount;
    private long baselineProfileCoverage;
    private boolean lazyInjectionEnabled;

    private double avgFrameProcessMs;
    private int qrScansPerMinute;
    private boolean autoTorchCalibrated;
    private double cameraInitMs;
    private int thermalThrottleEvents;

    private LocalDateTime lastHeartbeat;
    private LocalDateTime lastWakeUpSuccess;
    private int missedWakeUps;
    private int totalWakeAttempts;

    public enum DeviceRole {
        RESIDENT, GUARD, MC_MEMBER, DELIVERY
    }
}
