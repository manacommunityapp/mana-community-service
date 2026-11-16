package com.manacommunity.api.payments.analytics;

import com.manacommunity.api.payments.analytics.dto.*;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentAnalyticsService {

    private final PaymentOrderRepository repo;

    @Transactional(readOnly = true)
    public PaymentAnalyticsResponse getAnalytics(Long communityId, LocalDate from, LocalDate to) {
        OffsetDateTime fromDt = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDt   = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        long total      = repo.countByCommunityIdAndCreatedAtBetween(communityId, fromDt, toDt);
        long successful = repo.countByCommunityIdAndStatusAndCreatedAtBetween(
                communityId, PaymentOrderStatus.SUCCESS, fromDt, toDt);
        long failed     = repo.countByCommunityIdAndStatusAndCreatedAtBetween(
                communityId, PaymentOrderStatus.FAILED, fromDt, toDt);

        BigDecimal totalAmount = repo.sumAmountByCommunityIdAndStatusAndCreatedAtBetween(
                communityId, PaymentOrderStatus.SUCCESS, fromDt, toDt);
        BigDecimal totalRefunded = repo.sumRefundedAmountByCommunityIdAndCreatedAtBetween(
                communityId, fromDt, toDt);

        double successRate = total == 0 ? 0.0
                : BigDecimal.valueOf(successful * 100.0 / total)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();

        List<GatewayBreakdown> byGateway = repo.analyticsGroupByGateway(communityId, fromDt, toDt)
                .stream()
                .map(row -> {
                    long cnt        = ((Number) row[1]).longValue();
                    BigDecimal amt  = row[2] instanceof BigDecimal
                            ? (BigDecimal) row[2]
                            : new BigDecimal(row[2].toString());
                    long successes  = ((Number) row[3]).longValue();
                    double gwRate   = cnt == 0 ? 0.0
                            : BigDecimal.valueOf(successes * 100.0 / cnt)
                                .setScale(2, RoundingMode.HALF_UP).doubleValue();
                    return new GatewayBreakdown(row[0].toString(), cnt, amt, gwRate);
                })
                .collect(Collectors.toList());

        List<ModuleBreakdown> byModule = repo.analyticsGroupByReferenceType(communityId, fromDt, toDt)
                .stream()
                .map(row -> {
                    long cnt       = ((Number) row[1]).longValue();
                    BigDecimal amt = row[2] instanceof BigDecimal
                            ? (BigDecimal) row[2]
                            : new BigDecimal(row[2].toString());
                    return new ModuleBreakdown(row[0].toString(), cnt, amt);
                })
                .collect(Collectors.toList());

        List<DailyTrendPoint> trend = repo.dailyTrend(communityId, fromDt, toDt)
                .stream()
                .map(row -> {
                    long cnt       = ((Number) row[1]).longValue();
                    BigDecimal amt = row[2] instanceof BigDecimal
                            ? (BigDecimal) row[2]
                            : new BigDecimal(row[2].toString());
                    return new DailyTrendPoint(row[0].toString(), cnt, amt);
                })
                .collect(Collectors.toList());

        return PaymentAnalyticsResponse.builder()
                .totalOrders(total)
                .totalSuccessful(successful)
                .totalFailed(failed)
                .successRate(successRate)
                .totalAmount(totalAmount)
                .totalRefunded(totalRefunded)
                .breakdownByGateway(byGateway)
                .breakdownByModule(byModule)
                .dailyTrend(trend)
                .build();
    }
}
