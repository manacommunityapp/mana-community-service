package com.manacommunity.api.iot.metering;

import com.manacommunity.api.iot.metering.MeteringEnums.*;
import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "IotSmartMeter")
@Table(name = "smart_meters")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartMeter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meter_serial_number", nullable = false, unique = true, length = 100)
    private String meterSerialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false, length = 50)
    private MeterType meterType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "unit_id")
    private Long unitId;

    @Column(name = "unit_number", length = 50)
    private String unitNumber;

    @Column(name = "block_name", length = 50)
    private String blockName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private MeterProtocol protocol = MeterProtocol.MQTT;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "mqtt_topic", length = 200)
    private String mqttTopic;

    @Builder.Default
    @Column(name = "pulse_multiplier", nullable = false, precision = 10, scale = 4)
    private BigDecimal pulseMultiplier = BigDecimal.ONE;

    @Builder.Default
    @Column(name = "last_reading", nullable = false, precision = 14, scale = 4)
    private BigDecimal lastReading = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "last_pulse_count", nullable = false)
    private Long lastPulseCount = 0L;

    @Column(name = "last_telemetry_time")
    private LocalDateTime lastTelemetryTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private MeterStatus status = MeterStatus.ACTIVE;

    @Builder.Default
    @Column(name = "battery_level")
    private Integer batteryLevel = 100;

    @Builder.Default
    @Column(name = "leak_detected", nullable = false)
    private Boolean leakDetected = false;

    @Builder.Default
    @Column(name = "installed_at", nullable = false)
    private LocalDateTime installedAt = LocalDateTime.now();

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
