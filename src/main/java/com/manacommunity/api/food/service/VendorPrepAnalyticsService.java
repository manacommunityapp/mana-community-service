package com.manacommunity.api.food.service;

import com.manacommunity.api.food.entity.VendorPrepAnalytics;
import com.manacommunity.api.food.repository.VendorPrepAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorPrepAnalyticsService {

    private final VendorPrepAnalyticsRepository repo;

    @Transactional
    public void recordPrepStart(Long vendorId, Long communityId, Long orderId, String orderType, Instant orderPlacedAt) {
        VendorPrepAnalytics rec = repo.findByVendorIdAndOrderIdAndOrderType(vendorId, orderId, orderType)
            .orElse(VendorPrepAnalytics.builder().vendorId(vendorId).communityId(communityId)
                .orderId(orderId).orderType(orderType).orderPlacedAt(orderPlacedAt)
                .recordedAt(Instant.now()).build());
        rec.setPrepStartedAt(Instant.now());
        repo.save(rec);
    }

    @Transactional
    public void recordPrepComplete(Long vendorId, Long orderId, String orderType, Long slotId) {
        repo.findByVendorIdAndOrderIdAndOrderType(vendorId, orderId, orderType).ifPresent(rec -> {
            rec.setPrepCompletedAt(Instant.now());
            rec.setSlotId(slotId);
            // on-time = completed before slot cutoff (slot lookup skipped here, just log)
            rec.setWasOnTime(rec.getPrepStartedAt() != null); // simplified; real logic uses slot
            repo.save(rec);
        });
    }

    public VendorPrepSummary getAnalytics(Long vendorId, LocalDate from, LocalDate to) {
        Instant fromI = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toI = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<VendorPrepAnalytics> records = repo.findByVendorIdAndOrderPlacedAtBetween(vendorId, fromI, toI);
        long onTime = repo.countOnTime(vendorId, fromI, toI);
        long totalWithPrep = records.stream().filter(r -> r.getPrepStartedAt() != null && r.getPrepCompletedAt() != null).count();
        double avgPrepMinutes = records.stream()
            .filter(r -> r.getPrepStartedAt() != null && r.getPrepCompletedAt() != null)
            .mapToLong(r -> Duration.between(r.getPrepStartedAt(), r.getPrepCompletedAt()).toMinutes())
            .average().orElse(0.0);
        double onTimePercent = records.isEmpty() ? 0 : (onTime * 100.0 / records.size());
        return new VendorPrepSummary(avgPrepMinutes, onTimePercent, records.size(), records.size() - onTime);
    }

    public record VendorPrepSummary(double avgPrepMinutes, double onTimePercent, long totalOrders, long lateCount) {}
}
