package com.cpn.domain.analytics.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "analytics_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsMetric extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String metricType; // PROFILE_VIEW, SEARCH_APPEARANCE, POST_IMPRESSION

    private Integer count;
    private LocalDate recordedDate;
}
