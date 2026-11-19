package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.VendorPrepAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VendorPrepAnalyticsRepository extends JpaRepository<VendorPrepAnalytics, Long> {
    Optional<VendorPrepAnalytics> findByVendorIdAndOrderIdAndOrderType(Long vendorId, Long orderId, String orderType);
    List<VendorPrepAnalytics> findByVendorIdAndOrderPlacedAtBetween(Long vendorId, Instant from, Instant to);

    @Query("SELECT COUNT(v) FROM VendorPrepAnalytics v WHERE v.vendorId = :vendorId AND v.orderPlacedAt BETWEEN :from AND :to AND v.wasOnTime = true")
    long countOnTime(@Param("vendorId") Long vendorId, @Param("from") Instant from, @Param("to") Instant to);
}
