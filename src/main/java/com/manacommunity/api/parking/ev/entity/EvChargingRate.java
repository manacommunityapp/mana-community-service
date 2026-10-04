package com.manacommunity.api.parking.ev.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ev_charging_rate")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvChargingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "rate_name", nullable = false, length = 60)
    @Builder.Default
    private String rateName = "Standard Residential Rate";

    @Column(name = "unit_rate_per_kwh", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal unitRatePerKwh = new BigDecimal("8.50");

    @Column(name = "peak_rate_per_kwh", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal peakRatePerKwh = new BigDecimal("12.00");

    @Column(name = "peak_start_hour", nullable = false)
    @Builder.Default
    private int peakStartHour = 18; // 6 PM

    @Column(name = "peak_end_hour", nullable = false)
    @Builder.Default
    private int peakEndHour = 22; // 10 PM

    @Column(name = "minimum_bill_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumBillAmount = new BigDecimal("20.00");

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}