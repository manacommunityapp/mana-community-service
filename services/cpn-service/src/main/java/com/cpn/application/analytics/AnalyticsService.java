package com.cpn.application.analytics;

import com.cpn.domain.analytics.model.AnalyticsMetric;
import com.cpn.domain.analytics.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    @Transactional(readOnly = true)
    public List<AnalyticsMetric> getUserMetrics(UUID userId) {
        return analyticsRepository.findByUserId(userId);
    }

    @Transactional
    public AnalyticsMetric recordMetric(AnalyticsMetric metric) {
        return analyticsRepository.save(metric);
    }
}
