package com.manacommunity.api.mobile.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wake_up_audits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WakeUpAudit extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;
    private String oem;
    private String model;

    @Enumerated(EnumType.STRING)
    private WakeStrategy strategy;

    private long deliveryLatencyMs;
    private boolean wakeSuccessful;
    private String failureReason;

    @Enumerated(EnumType.STRING)
    private PushPriority pushPriority;

    private boolean foregroundServiceActive;
    private boolean voipPushUsed;
    private int retryAttempts;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;

    public enum WakeStrategy {
        FCM_HIGH_PRIORITY, FCM_DATA_MESSAGE, FOREGROUND_SERVICE,
        VOIP_PUSH_KIT, WEBSOCKET_KEEPALIVE, ALARM_MANAGER_EXACT,
        WORK_MANAGER_EXPEDITED, DUAL_CHANNEL
    }

    public enum PushPriority {
        HIGH, NORMAL, LOW
    }
}
