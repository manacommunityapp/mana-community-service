package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "smart_meters",
        uniqueConstraints = @UniqueConstraint(name = "uk_meter_serial", columnNames = {"meter_serial"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartMeter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "flat_number", nullable = false, length = 20)
    private String flatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false, length = 20)
    private MeterType meterType;

    @Column(name = "meter_serial", nullable = false, length = 50)
    private String meterSerial;

    @Column(length = 100)
    private String location;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "last_reading", precision = 14, scale = 2)
    private BigDecimal lastReading;

    @Column(name = "last_reading_date")
    private LocalDate lastReadingDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MeterType {
        ELECTRICITY, WATER, GAS
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) active = true;
    }
}
