package com.manacommunity.api.platform.bizrev;

import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.repository.PaymentOrderRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessRevenueDashboardService {

    private final CommunityRepository communityRepo;
    private final VendorRepository vendorRepo;
    private final PaymentOrderRepository paymentOrderRepo;

    private final AtomicReference<BusinessRevenueDashboardSnapshot> cache = new AtomicReference<>();

    @Transactional(readOnly = true)
    public BusinessRevenueDashboardSnapshot getSnapshot() {
        BusinessRevenueDashboardSnapshot snap = cache.get();
        return snap != null ? snap : refreshSnapshot();
    }

    @Transactional(readOnly = true)
    public BusinessRevenueDashboardSnapshot refreshSnapshot() {
        BusinessRevenueDashboardSnapshot snap = buildSnapshot();
        cache.set(snap);
        return snap;
    }

    private BusinessRevenueDashboardSnapshot buildSnapshot() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate prevMonthStart = monthStart.minusMonths(1);
        LocalDate prevMonthEnd = monthStart.minusDays(1);

        OffsetDateTime fromDt = monthStart.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDt = today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime prevFromDt = prevMonthStart.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime prevToDt = monthStart.atStartOfDay().atOffset(ZoneOffset.UTC);

        // 1. Active communities
        long totalCommunities = safeCount(() -> communityRepo.count());
        long activeCommunities = safeCount(() -> (long) communityRepo.findByActiveTrueOrderByNameAsc().size());
        double activationRate = totalCommunities > 0 ? (activeCommunities * 100.0 / totalCommunities) : 0.0;

        // 2. MRR — sum of this month's successful payment volume
        BigDecimal mrr = computePlatformGmv(fromDt, toDt);
        BigDecimal prevMrr = computePlatformGmv(prevFromDt, prevToDt);
        BigDecimal mrrGrowth = prevMrr.compareTo(BigDecimal.ZERO) > 0
            ? mrr.subtract(prevMrr).multiply(BigDecimal.valueOf(100)).divide(prevMrr, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // 3. GMV — same as MRR for now (total successful payment volume this month)
        BigDecimal gmv = mrr;
        BigDecimal prevGmv = prevMrr;
        BigDecimal gmvGrowth = mrrGrowth;

        // 4. Commission (5% of GMV as platform take-rate)
        BigDecimal commission = gmv.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
        double commissionMargin = gmv.compareTo(BigDecimal.ZERO) > 0
            ? commission.multiply(BigDecimal.valueOf(100)).divide(gmv, 2, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        // 5. Vendors
        long activeVendors = safeCount(() -> (long) vendorRepo.findByActiveTrueOrderByNameAsc().size());
        // TODO: Vendor entity has no createdAt-based monthly filter; use 0
        long newVendors = 0L;

        // 6. Completed orders — count SUCCESS payments as proxy
        long completedOrders = computePlatformCount(PaymentOrderStatus.SUCCESS, fromDt, toDt);
        // TODO: amenity bookings table has no payment link yet
        long completedBookings = 0L;

        // 7. Payment failure rate
        long totalPayments = computePlatformTotalCount(fromDt, toDt);
        long failedPayments = computePlatformCount(PaymentOrderStatus.FAILED, fromDt, toDt);
        double failRate = totalPayments > 0 ? (failedPayments * 100.0 / totalPayments) : 0.0;

        // 8. Refund rate
        BigDecimal refundAmt = computePlatformRefunds(fromDt, toDt);
        double refundRate = gmv.compareTo(BigDecimal.ZERO) > 0
            ? refundAmt.multiply(BigDecimal.valueOf(100)).divide(gmv, 2, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        // 9. Repeat purchase rate — TODO: needs DISTINCT userId GROUP BY; placeholder
        long repeatBuyers = 0L;
        long totalBuyers = 0L;
        double repeatRate = 0.0;

        // 10. Vendor settlement delay — no settlement entity with delay yet
        double avgSettlementDelay = 0.0;
        long overdueSettlements = 0L;

        // 11. Module margins
        List<BusinessRevenueDashboardSnapshot.ModuleMargin> moduleMargins =
            buildModuleMargins(fromDt, toDt);

        return new BusinessRevenueDashboardSnapshot(
            Instant.now(), monthStart, today,
            activeCommunities, totalCommunities, activationRate,
            mrr, mrrGrowth,
            gmv, gmvGrowth,
            commission, commissionMargin,
            activeVendors, newVendors,
            completedOrders, completedBookings,
            failRate, failedPayments, totalPayments,
            refundRate, refundAmt,
            repeatRate, repeatBuyers, totalBuyers,
            avgSettlementDelay, overdueSettlements,
            moduleMargins
        );
    }

    /**
     * Compute platform-wide GMV by iterating all active communities.
     * PaymentOrderRepository does not accept null communityId.
     */
    private BigDecimal computePlatformGmv(OffsetDateTime from, OffsetDateTime to) {
        try {
            List<BigDecimal> totals = new ArrayList<>();
            for (var community : communityRepo.findByActiveTrueOrderByNameAsc()) {
                try {
                    BigDecimal amt = paymentOrderRepo.sumAmountByCommunityIdAndStatusAndCreatedAtBetween(
                        community.getId(), PaymentOrderStatus.SUCCESS, from, to);
                    if (amt != null) totals.add(amt);
                } catch (Exception e) {
                    log.debug("GMV calc skipped for community {}: {}", community.getId(), e.getMessage());
                }
            }
            return totals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Platform GMV calc failed: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Compute platform-wide count of orders with a given status by iterating communities.
     */
    private long computePlatformCount(PaymentOrderStatus status, OffsetDateTime from, OffsetDateTime to) {
        long total = 0L;
        try {
            for (var community : communityRepo.findByActiveTrueOrderByNameAsc()) {
                try {
                    total += paymentOrderRepo.countByCommunityIdAndStatusAndCreatedAtBetween(
                        community.getId(), status, from, to);
                } catch (Exception e) {
                    log.debug("Count skipped for community {}: {}", community.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Platform count failed for status {}: {}", status, e.getMessage());
        }
        return total;
    }

    /**
     * Compute platform-wide total payment count by iterating communities.
     */
    private long computePlatformTotalCount(OffsetDateTime from, OffsetDateTime to) {
        long total = 0L;
        try {
            for (var community : communityRepo.findByActiveTrueOrderByNameAsc()) {
                try {
                    total += paymentOrderRepo.countByCommunityIdAndCreatedAtBetween(
                        community.getId(), from, to);
                } catch (Exception e) {
                    log.debug("Total count skipped for community {}: {}", community.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Platform total count failed: {}", e.getMessage());
        }
        return total;
    }

    /**
     * Compute platform-wide refunded amount by iterating communities.
     */
    private BigDecimal computePlatformRefunds(OffsetDateTime from, OffsetDateTime to) {
        BigDecimal total = BigDecimal.ZERO;
        try {
            for (var community : communityRepo.findByActiveTrueOrderByNameAsc()) {
                try {
                    BigDecimal amt = paymentOrderRepo.sumRefundedAmountByCommunityIdAndCreatedAtBetween(
                        community.getId(), from, to);
                    if (amt != null) total = total.add(amt);
                } catch (Exception e) {
                    log.debug("Refund calc skipped for community {}: {}", community.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Platform refund calc failed: {}", e.getMessage());
        }
        return total;
    }

    private List<BusinessRevenueDashboardSnapshot.ModuleMargin> buildModuleMargins(
            OffsetDateTime from, OffsetDateTime to) {
        List<BusinessRevenueDashboardSnapshot.ModuleMargin> result = new ArrayList<>();
        try {
            for (var community : communityRepo.findByActiveTrueOrderByNameAsc()) {
                try {
                    List<Object[]> rows = paymentOrderRepo.analyticsGroupByReferenceType(
                        community.getId(), from, to);
                    for (Object[] row : rows) {
                        String module = row[0] != null ? row[0].toString() : "UNKNOWN";
                        BigDecimal rev = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
                        // Cost = 2% gateway MDR
                        BigDecimal cost = rev.multiply(BigDecimal.valueOf(0.02)).setScale(2, RoundingMode.HALF_UP);
                        BigDecimal margin = rev.subtract(cost);
                        double marginPct = rev.compareTo(BigDecimal.ZERO) > 0
                            ? margin.multiply(BigDecimal.valueOf(100)).divide(rev, 2, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                        result.add(new BusinessRevenueDashboardSnapshot.ModuleMargin(
                            module, rev, cost, margin, marginPct));
                    }
                } catch (Exception e) {
                    log.debug("Module margins skipped for community {}: {}", community.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Module margins calc failed: {}", e.getMessage());
        }

        // Merge rows by module name
        return result.stream()
            .collect(Collectors.groupingBy(
                BusinessRevenueDashboardSnapshot.ModuleMargin::module,
                Collectors.reducing(
                    null,
                    (a, b) -> {
                        if (a == null) return b;
                        BigDecimal combinedRev = a.revenue().add(b.revenue());
                        BigDecimal combinedCost = a.cost().add(b.cost());
                        BigDecimal combinedMargin = a.contributionMargin().add(b.contributionMargin());
                        double combinedMarginPct = combinedRev.compareTo(BigDecimal.ZERO) > 0
                            ? combinedMargin.multiply(BigDecimal.valueOf(100))
                                .divide(combinedRev, 2, RoundingMode.HALF_UP)
                                .doubleValue()
                            : 0.0;
                        return new BusinessRevenueDashboardSnapshot.ModuleMargin(
                            a.module(), combinedRev, combinedCost, combinedMargin, combinedMarginPct);
                    }
                )
            ))
            .values().stream()
            .filter(m -> m != null)
            .sorted((a, b) -> b.revenue().compareTo(a.revenue()))
            .collect(Collectors.toList());
    }

    private long safeCount(java.util.concurrent.Callable<Long> fn) {
        try { return fn.call(); } catch (Exception e) { return 0L; }
    }

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void scheduledRefresh() {
        try {
            refreshSnapshot();
        } catch (Exception e) {
            log.warn("Business revenue snapshot refresh failed: {}", e.getMessage());
        }
    }
}
