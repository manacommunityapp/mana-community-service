package com.manacommunity.platform.ops;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "platform_metrics_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlatformMetricsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_type", nullable = false, length = 80)
    private String metricType;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal value;

    @Column(length = 30)
    private String unit;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant recordedAt = Instant.now();
}
