package com.manacommunity.api.iot.metering;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_telemetry_readings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterTelemetryReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    private SmartMeter meter;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "raw_pulse_count", nullable = false)
    private Long rawPulseCount;

    @Column(name = "cumulative_consumption", nullable = false, precision = 14, scale = 4)
    private BigDecimal cumulativeConsumption;

    @Column(name = "delta_consumption", nullable = false, precision = 14, scale = 4)
    private BigDecimal deltaConsumption;

    @Column(name = "instantaneous_flow", precision = 10, scale = 4)
    private BigDecimal instantaneousFlow;

    @Column(precision = 8, scale = 2)
    private BigDecimal voltage;

    @Column(precision = 8, scale = 2)
    private BigDecimal current;

    @Column(name = "power_factor", precision = 4, scale = 2)
    private BigDecimal powerFactor;

    @Builder.Default
    @Column(name = "tamper_flag", nullable = false)
    private Boolean tamperFlag = false;

    @Column(name = "signal_rssi")
    private Integer signalRssi;

    @Column(name = "raw_payload_json", columnDefinition = "TEXT")
    private String rawPayloadJson;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
