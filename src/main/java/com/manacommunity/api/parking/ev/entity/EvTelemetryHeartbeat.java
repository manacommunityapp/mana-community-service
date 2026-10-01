package com.manacommunity.api.parking.ev.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ev_telemetry_heartbeats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvTelemetryHeartbeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "charger_id", nullable = false)
    private Long chargerId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "power_kw", precision = 8, scale = 2)
    private BigDecimal powerKw;

    @Column(precision = 6, scale = 2)
    private BigDecimal voltage;

    @Column(name = "current_amps", precision = 6, scale = 2)
    private BigDecimal currentAmps;

    @Column(name = "temperature_celsius", precision = 5, scale = 1)
    private BigDecimal temperatureCelsius;

    @Column(name = "meter_reading_kwh")
    private Double meterReadingKwh;

    @Column(name = "state_of_charge_percent", precision = 5, scale = 2)
    private BigDecimal stateOfChargePercent;

    @Column(name = "error_code", length = 64)
    private String errorCode;
}
