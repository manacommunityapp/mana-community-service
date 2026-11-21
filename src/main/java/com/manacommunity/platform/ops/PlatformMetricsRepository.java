package com.manacommunity.platform.ops;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PlatformMetricsRepository extends JpaRepository<PlatformMetricsHistory, Long> {

    List<PlatformMetricsHistory> findByMetricTypeAndRecordedAtAfterOrderByRecordedAtDesc(
        String metricType, Instant after);

    @Query("SELECT p FROM PlatformMetricsHistory p WHERE p.metricType = :type " +
           "ORDER BY p.recordedAt DESC LIMIT 1")
    java.util.Optional<PlatformMetricsHistory> findLatestByType(@Param("type") String type);
}
