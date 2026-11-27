package com.manacommunity.api.analytics.service;

import com.manacommunity.api.analytics.dto.BusinessRevenueSummary;
import com.manacommunity.api.analytics.entity.CommunitySaasSubscription;
import com.manacommunity.api.analytics.repository.CommunitySaasSubscriptionRepository;
import com.manacommunity.api.analytics.repository.BusinessModuleFinanceDailyRepository;
import com.manacommunity.api.analytics.config.BusinessRevenueProperties;
import com.manacommunity.api.booking.repository.ResourceBookingRepository;
import com.manacommunity.api.commerce.core.model.CommerceSettlement;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.commerce.core.repository.CommerceSettlementRepository;
import com.manacommunity.api.marketplace.entity.MarketplaceSettlementRecord;
import com.manacommunity.api.marketplace.repository.MarketOrderRepository;
import com.manacommunity.api.marketplace.repository.MarketplaceSettlementRepository;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.repository.PaymentOrderRepository;
import com.manacommunity.api.repository.VendorRepository;
import com.manacommunity.api.vendor.entity.VmsPayment;
import com.manacommunity.api.vendor.entity.VmsSettlement;
import com.manacommunity.api.vendor.repository.VmsPaymentRepository;
import com.manacommunity.api.vendor.repository.VmsSettlementRepository;
import com.manacommunity.api.vendor.repository.VmsBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BusinessRevenueService {
    private static final List<String> MODULES = List.of(
            "MAINTENANCE_BILL", "EVENT_REGISTRATION", "POOJA_BOOKING", "SPORTS_REGISTRATION",
            "FOOD_ORDER", "FOOD_GROCERY_ORDER", "MARKETPLACE_ORDER", "HEALTH_BOOKING",
            "AMENITY_BOOKING", "TRIP_SETTLEMENT", "HOME_SERVICE", "VENDOR_BOOKING", "BBPS_BILL", "MANUAL");

    private final PaymentOrderRepository paymentOrderRepo;
    private final VendorRepository vendorRepo;
    private final CommerceOrderRepository commerceOrderRepo;
    private final MarketOrderRepository marketOrderRepo;
    private final ResourceBookingRepository resourceBookingRepo;
    private final CommerceSettlementRepository commerceSettlementRepo;
    private final VmsPaymentRepository vmsPaymentRepo;
    private final VmsSettlementRepository vmsSettlementRepo;
    private final VmsBookingRepository vmsBookingRepo;
    private final MarketplaceSettlementRepository marketplaceSettlementRepo;
    private final CommunitySaasSubscriptionRepository communitySubscriptionRepo;
    private final BusinessRevenueProperties businessProperties;
    private final BusinessModuleFinanceDailyRepository moduleFinanceRepo;

    @Transactional(readOnly = true)
    public BusinessRevenueSummary getSummary(LocalDate from, LocalDate to,
                                            Double gatewayMdrRate, Double perTxnFixedCost,
                                            Double moduleHostingAllocation) {
        OffsetDateTime fromOffset = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toOffset = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);
        LocalDateTime fromLocal = from.atStartOfDay();
        LocalDateTime toLocal = to.plusDays(1).atStartOfDay().minusNanos(1);
        Instant fromInstant = fromOffset.toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().minusNanos(1);

        BigDecimal mdrRate = nonNegative(gatewayMdrRate, 0.02);
        BigDecimal fixedCost = nonNegative(perTxnFixedCost, 2.00);
        BigDecimal hosting = nonNegative(moduleHostingAllocation, 0.00);

        long started = paymentOrderRepo.countByCreatedAtBetween(fromOffset, toOffset);
        long succeeded = paymentOrderRepo.countByStatusAndCreatedAtBetween(PaymentOrderStatus.SUCCESS, fromOffset, toOffset);
        long failed = paymentOrderRepo.countByStatusAndCreatedAtBetween(PaymentOrderStatus.FAILED, fromOffset, toOffset)
                + paymentOrderRepo.countByStatusAndCreatedAtBetween(PaymentOrderStatus.EXPIRED, fromOffset, toOffset)
                + paymentOrderRepo.countByStatusAndCreatedAtBetween(PaymentOrderStatus.VOIDED, fromOffset, toOffset);

        BigDecimal gmv = zero(paymentOrderRepo.sumSuccessfulAmountBetween(fromOffset, toOffset));
        BigDecimal refundedAmount = zero(paymentOrderRepo.sumRefundedAmountBetween(fromOffset, toOffset));
        long refundCount = paymentOrderRepo.countOrdersWithRefundBetween(fromOffset, toOffset);
        long completedOrders = commerceOrderRepo.countCompletedPlatformOrders(fromLocal, toLocal)
                + marketOrderRepo.countCompletedPlatformOrders(fromLocal, toLocal);
        long completedBookings = resourceBookingRepo.countCompletedPlatformBookings(fromLocal, toLocal)
                + vmsBookingRepo.countCompletedPlatformBookings(fromLocal, toLocal);
        BuyerCounts buyers = buyerCounts(fromLocal, toLocal);
        List<CommunitySaasSubscription> activeSubscriptions = communitySubscriptionRepo.findCurrentlyPaying(LocalDate.now());
        BigDecimal subscriptionMrr = activeSubscriptions.stream()
                .map(subscription -> subscription.getRecurringAmount().divide(
                        BigDecimal.valueOf(subscription.getBillingPeriodMonths()), 8, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        long activePayingCommunities = activeSubscriptions.stream().map(subscription -> subscription.getCommunity().getId()).distinct().count();

        long activeVendors = vendorRepo.countByActiveTrue();
        List<Object[]> byModule = paymentOrderRepo.successfulAnalyticsGroupByReferenceType(fromOffset, toOffset);
        Map<String, ModuleTotals> totals = new HashMap<>();
        for (Object[] row : byModule) {
            String module = String.valueOf(row[0]);
            totals.put(module, new ModuleTotals(((Number) row[1]).longValue(), decimal(row[2]), ((Number) row[3]).longValue()));
        }
        Set<String> allModules = new LinkedHashSet<>(MODULES);
        allModules.addAll(totals.keySet());
        Map<String, ModuleFinanceTotals> moduleFinance = new HashMap<>();
        for (Object[] row : moduleFinanceRepo.aggregateForPeriod(from, to)) {
            moduleFinance.put(String.valueOf(row[0]), new ModuleFinanceTotals(decimal(row[1]), decimal(row[2])));
            allModules.add(String.valueOf(row[0]));
        }
        List<BusinessRevenueSummary.Module> modules = allModules.stream().map(module -> {
            ModuleTotals total = totals.getOrDefault(module, new ModuleTotals(0, BigDecimal.ZERO, 0));
            BigDecimal cost = total.grossPaymentVolume().multiply(mdrRate).add(fixedCost.multiply(BigDecimal.valueOf(total.orders())))
                    .add(total.orders() > 0 || total.grossPaymentVolume().signum() > 0 ? hosting : BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            ModuleFinanceTotals actual = moduleFinance.get(module);
            return BusinessRevenueSummary.Module.builder().module(module).activeUsers(total.users())
                    .orderCount(total.orders()).grossPaymentVolume(total.grossPaymentVolume()).estimatedDirectCost(cost)
                    .actualCommissionRevenue(actual != null ? actual.commissionRevenue() : null)
                    .actualDirectCost(actual != null ? actual.directCost() : null)
                    .marginSource(actual != null ? "ACTUAL" : "GMV_PROXY").build();
        }).toList();

        List<String> unavailableMetrics = new ArrayList<>();

        Integer settlementSlaDays = businessProperties.getVendorSettlementSlaDays();
        Long overdueSettlements = null;
        if (settlementSlaDays == null || settlementSlaDays <= 0) {
            unavailableMetrics.add("overdueSettlements: configure mana.business.vendor-settlement-sla-days");
        } else {
            Instant instantCutoff = Instant.now().minus(Duration.ofDays(settlementSlaDays));
            LocalDate dateCutoff = LocalDate.now().minusDays(settlementSlaDays);
            LocalDateTime dateTimeCutoff = LocalDateTime.now().minusDays(settlementSlaDays);
            overdueSettlements = commerceSettlementRepo.countOverduePlatformSettlements(instantCutoff)
                    + vmsSettlementRepo.countOverduePlatformSettlements(dateCutoff)
                    + marketplaceSettlementRepo.countOverduePlatformSettlements(dateTimeCutoff);
        }
        if (moduleFinance.isEmpty()) {
            unavailableMetrics.add("contributionMarginByModule: no module finance entries yet; displayed margin uses estimated GMV-based costs");
        } else {
            unavailableMetrics.add("contributionMarginByModule: modules without finance entries use the GMV-based cost proxy");
        }

        return BusinessRevenueSummary.builder()
                .transactions(BusinessRevenueSummary.Transactions.builder().paymentsStarted(started)
                        .paymentsSucceeded(succeeded).paymentsFailed(failed).build())
                .revenue(BusinessRevenueSummary.Revenue.builder().gmv(gmv).commission(commission(from, to, fromLocal, toLocal, fromInstant, toInstant))
                        .subscriptionMrr(subscriptionMrr)
                        .subscribers((long) activeSubscriptions.size()).build())
                .business(BusinessRevenueSummary.Business.builder().activePayingCommunities(activePayingCommunities)
                        .activeVendors(activeVendors).completedOrders(completedOrders).completedBookings(completedBookings)
                        .refundCount(refundCount).refundedAmount(refundedAmount).repeatBuyers(buyers.repeat())
                        .uniqueBuyers(buyers.unique()).averageSettlementDelayDays(averageSettlementDelay(from, to, fromLocal, toLocal, fromInstant, toInstant))
                        .overdueSettlements(overdueSettlements).settlementSlaDays(settlementSlaDays).build())
                .modules(modules)
                .costCalibration(BusinessRevenueSummary.CostCalibration.builder().gatewayMdrRate(mdrRate.doubleValue())
                        .perTxnFixedCost(fixedCost.doubleValue()).moduleHostingAllocation(hosting.doubleValue()).build())
                .unavailableMetrics(unavailableMetrics)
                .build();
    }

    private BigDecimal commission(LocalDate from, LocalDate to, LocalDateTime fromLocal, LocalDateTime toLocal,
                                  Instant fromInstant, Instant toInstant) {
        BigDecimal commerce = commerceSettlementRepo.findByCycleEndDateBetween(fromInstant, toInstant).stream()
                .map(CommerceSettlement::getPlatformFee).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vms = zero(vmsPaymentRepo.sumCommissionAmountPlatform(from, to, VmsPayment.PaymentProcessStatus.COMPLETED));
        BigDecimal marketplace = marketplaceSettlementRepo.findByCreatedAtBetween(fromLocal, toLocal).stream()
                .filter(s -> s.getStatus() != MarketplaceSettlementRecord.SettlementStatus.FAILED
                        && s.getStatus() != MarketplaceSettlementRecord.SettlementStatus.REVERSED)
                .map(MarketplaceSettlementRecord::getPlatformFee).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return commerce.add(vms).add(marketplace).setScale(2, RoundingMode.HALF_UP);
    }

    private Double averageSettlementDelay(LocalDate from, LocalDate to, LocalDateTime fromLocal, LocalDateTime toLocal,
                                          Instant fromInstant, Instant toInstant) {
        List<Double> days = new ArrayList<>();
        for (var s : commerceSettlementRepo.findByCycleEndDateBetween(fromInstant, toInstant)) {
            if (s.getCycleEndDate() != null && s.getPayoutDate() != null) addDelay(days, s.getCycleEndDate(), s.getPayoutDate());
        }
        for (VmsSettlement s : vmsSettlementRepo.findByPeriodEndBetween(from, to)) {
            if (s.getPeriodEnd() != null && s.getPaidAt() != null)
                addDelay(days, s.getPeriodEnd().plusDays(1).atStartOfDay(), s.getPaidAt());
        }
        for (MarketplaceSettlementRecord s : marketplaceSettlementRepo.findByCreatedAtBetween(fromLocal, toLocal)) {
            if (s.getCreatedAt() != null && s.getSettledAt() != null) addDelay(days, s.getCreatedAt(), s.getSettledAt());
        }
        return days.isEmpty() ? null : BigDecimal.valueOf(days.stream().mapToDouble(Double::doubleValue).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void addDelay(List<Double> delays, java.time.temporal.Temporal start, java.time.temporal.Temporal end) {
        Duration duration = Duration.between(start, end);
        if (!duration.isNegative()) delays.add(duration.toSeconds() / 86400.0);
    }

    private BuyerCounts buyerCounts(LocalDateTime from, LocalDateTime to) {
        Map<Long, Long> byBuyer = new HashMap<>();
        merge(byBuyer, commerceOrderRepo.completedOrderCountsByBuyer(from, to));
        merge(byBuyer, marketOrderRepo.completedOrderCountsByBuyer(from, to));
        return new BuyerCounts(byBuyer.size(), byBuyer.values().stream().filter(count -> count > 1).count());
    }

    private void merge(Map<Long, Long> result, List<Object[]> rows) {
        for (Object[] row : rows) {
            if (row[0] == null) continue;
            result.merge(((Number) row[0]).longValue(), ((Number) row[1]).longValue(), Long::sum);
        }
    }

    private BigDecimal nonNegative(Double value, double fallback) {
        return value != null && Double.isFinite(value) && value >= 0 ? BigDecimal.valueOf(value) : BigDecimal.valueOf(fallback);
    }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private BigDecimal decimal(Object value) { return value instanceof BigDecimal b ? b : new BigDecimal(value.toString()); }
    private record ModuleTotals(long orders, BigDecimal grossPaymentVolume, long users) {}
    private record ModuleFinanceTotals(BigDecimal commissionRevenue, BigDecimal directCost) {}
    private record BuyerCounts(long unique, long repeat) {}
}
