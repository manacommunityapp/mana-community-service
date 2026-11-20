package com.manacommunity.api.iot.mesh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sensor_alert_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SensorAlertConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false)
    private Long sensorId;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType; // THRESHOLD_HIGH, THRESHOLD_LOW, RATE_CHANGE, OFFLINE

    @Column(name = "threshold_value", precision = 15, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "rate_change_pct", precision = 5, scale = 2)
    private BigDecimal rateChangePct;

    @Column(name = "window_minutes", nullable = false)
    private int windowMinutes = 60;

    @Column(nullable = false, length = 20)
    private String severity = "WARNING";

    @Column(name = "notify_roles", nullable = false, length = 200)
    private String notifyRoles = "FM,ADMIN";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
