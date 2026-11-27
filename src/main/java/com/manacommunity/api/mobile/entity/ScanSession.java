package com.manacommunity.api.mobile.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scan_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScanSession extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;
    private String guardId;
    private String gateId;

    private int totalScans;
    private int successfulScans;
    private int failedScans;
    private double avgDecodeMs;
    private double p95DecodeMs;
    private double p99DecodeMs;
    private double avgFrameProcessMs;

    @Enumerated(EnumType.STRING)
    private LightingCondition lightingCondition;

    private boolean autoTorchTriggered;
    private int thermalThrottleEvents;
    private double avgDeviceTempC;
    private double peakDeviceTempC;

    private int cameraRestarts;
    private double cameraInitMs;
    private int droppedFrames;
    private int processedFrames;
    private double frameDropRate;

    private LocalDateTime sessionStart;
    private LocalDateTime sessionEnd;

    public enum LightingCondition {
        BRIGHT_SUNLIGHT, NORMAL, DIM, DARK, MIXED_GLARE
    }
}
