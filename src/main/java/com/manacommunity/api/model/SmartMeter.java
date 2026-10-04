package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "smart_meters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartMeter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "unit_number", length = 50)
    private String unitNumber;

    @Column(name = "meter_number", nullable = false, unique = true, length = 50)
    private String meterNumber;

    @Column(name = "meter_type", nullable = false, length = 20)
    private String meterType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "current_reading")
    private Double currentReading;

    @Column(name = "unit_of_measure", nullable = false, length = 10)
    private String unitOfMeasure;

    @Column(name = "pulse_multiplier")
    private Double pulseMultiplier;

    @Column(name = "last_reading_at")
    private LocalDateTime lastReadingAt;

    @Column(name = "burst_leak_detected")
    private Boolean burstLeakDetected;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "ACTIVE";
        if (currentReading == null) currentReading = 0.0;
        if (pulseMultiplier == null) pulseMultiplier = 1.0;
        if (burstLeakDetected == null) burstLeakDetected = false;
    }
}
