package com.cpn.domain.analytics.repository;

import com.cpn.domain.analytics.model.AnalyticsMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalyticsRepository extends JpaRepository<AnalyticsMetric, UUID> {
    List<AnalyticsMetric> findByUserId(UUID userId);
    List<AnalyticsMetric> findByUserIdAndMetricType(UUID userId, String metricType);
}
