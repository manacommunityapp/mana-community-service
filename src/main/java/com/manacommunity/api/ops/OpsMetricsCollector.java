package com.manacommunity.api.ops;

import com.manacommunity.api.ops.dto.OpsDashboardDtos.FailedRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class OpsMetricsCollector {

    private static final int MAX_FAILED_REQUESTS_RECORDED = 60;

    private final Deque<FailedRequestDto> failedRequests = new ConcurrentLinkedDeque<>();
    private final AtomicLong totalRequestsCount = new AtomicLong(0);
    private final AtomicLong total4xxCount = new AtomicLong(0);
    private final AtomicLong total5xxCount = new AtomicLong(0);

    public void recordRequest(String method, String path, int statusCode, long durationMs,
                              String correlationId, String clientIp, String errorMessage) {
        totalRequestsCount.incrementAndGet();

        if (statusCode >= 400 && statusCode < 500) {
            total4xxCount.incrementAndGet();
        } else if (statusCode >= 500) {
            total5xxCount.incrementAndGet();

            FailedRequestDto failure = FailedRequestDto.builder()
                    .id(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .method(method)
                    .path(path)
                    .statusCode(statusCode)
                    .latencyMs(durationMs)
                    .correlationId(correlationId != null ? correlationId : "-")
                    .clientIp(clientIp != null ? clientIp : "127.0.0.1")
                    .errorType("HTTP_" + statusCode)
                    .errorMessage(errorMessage != null ? errorMessage : "Server error: HTTP " + statusCode)
                    .build();

            failedRequests.addFirst(failure);
            while (failedRequests.size() > MAX_FAILED_REQUESTS_RECORDED) {
                failedRequests.pollLast();
            }
        }
    }

    public List<FailedRequestDto> getRecentFailures() {
        return new ArrayList<>(failedRequests);
    }

    public long getTotalRequests() {
        return totalRequestsCount.get();
    }

    public long getTotal5xxErrors() {
        return total5xxCount.get();
    }

    public long getTotal4xxErrors() {
        return total4xxCount.get();
    }

    public double getSlidingErrorRate() {
        long total = totalRequestsCount.get();
        if (total == 0) return 0.0;
        long failures = total5xxCount.get();
        double rate = ((double) failures / total) * 100.0;
        return Math.round(rate * 100.0) / 100.0;
    }

    public void resetMetrics() {
        failedRequests.clear();
        totalRequestsCount.set(0);
        total4xxCount.set(0);
        total5xxCount.set(0);
    }
}