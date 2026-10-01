package com.manacommunity.api.parking.ev.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "ev_tariff_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvTariffConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long communityId;

    @Column(nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal baseRatePerKwh = new BigDecimal("8.50");

    @Column(nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal peakHourRatePerKwh = new BigDecimal("12.00");

    @Builder.Default
    private LocalTime peakHourStart = LocalTime.of(18, 0); // 6:00 PM

    @Builder.Default
    private LocalTime peakHourEnd = LocalTime.of(22, 0); // 10:00 PM

    @Column(nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal idlePenaltyPerMinute = new BigDecimal("2.00");

    @Builder.Default
    private Integer gracePeriodMinutes = 15;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
