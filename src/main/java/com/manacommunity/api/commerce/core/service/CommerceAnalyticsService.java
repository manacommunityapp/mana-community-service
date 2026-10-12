package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceAnalyticsDto;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommerceAnalyticsService {

    private final CommerceOrderRepository orderRepository;

    @Transactional(readOnly = true)
    public CommerceAnalyticsDto getAnalytics(AppUser admin, String fromDate, String toDate) {
        Long communityId = admin.getCommunity().getId();

        Instant from = fromDate != null
                ? LocalDate.parse(fromDate).atStartOfDay().toInstant(ZoneOffset.UTC)
                : Instant.now().minusSeconds(30L * 24 * 3600);
        Instant to = toDate != null
                ? LocalDate.parse(toDate).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                : Instant.now();

        long total = orderRepository.countOrdersInPeriod(communityId, from, to);
        long completed = orderRepository.countCompletedInPeriod(communityId, from, to);
        long cancelled = orderRepository.countCancelledInPeriod(communityId, from, to);
        BigDecimal revenue = orderRepository.sumRevenueInPeriod(communityId, from, to);
        if (revenue == null) revenue = BigDecimal.ZERO;

        double fulfillmentRate = total > 0 ? (double) completed / total * 100 : 0;
        BigDecimal avgOrderValue = completed > 0
                ? revenue.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Object[]> topRows = orderRepository.findTopSkusByQuantity(communityId, from, to, PageRequest.of(0, 5));
        List<CommerceAnalyticsDto.TopProductDto> topProducts = topRows.stream()
                .map(r -> CommerceAnalyticsDto.TopProductDto.builder()
                        .sku(r[0] != null ? r[0].toString() : "")
                        .title(r[1] != null ? r[1].toString() : "")
                        .quantitySold(r[2] != null ? ((Number) r[2]).longValue() : 0)
                        .build())
                .collect(Collectors.toList());

        return CommerceAnalyticsDto.builder()
                .periodFrom(from.toString())
                .periodTo(to.toString())
                .totalOrders(total)
                .completedOrders(completed)
                .cancelledOrders(cancelled)
                .fulfillmentRate(Math.round(fulfillmentRate * 10.0) / 10.0)
                .totalRevenue(revenue)
                .averageOrderValue(avgOrderValue)
                .topProducts(topProducts)
                .build();
    }
}
