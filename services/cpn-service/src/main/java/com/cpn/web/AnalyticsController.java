package com.cpn.web;

import com.cpn.application.analytics.AnalyticsService;
import com.cpn.domain.analytics.model.AnalyticsMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpn/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AnalyticsMetric>> getUserMetrics(@PathVariable UUID userId) {
        return ResponseEntity.ok(analyticsService.getUserMetrics(userId));
    }

    @PostMapping
    public ResponseEntity<AnalyticsMetric> recordMetric(@RequestBody AnalyticsMetric metric) {
        return ResponseEntity.ok(analyticsService.recordMetric(metric));
    }
}
