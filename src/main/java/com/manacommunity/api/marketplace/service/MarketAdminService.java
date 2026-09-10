package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.marketplace.dto.MarketAdminMetricsDto;
import com.manacommunity.api.marketplace.dto.MarketAuditLogDto;
import com.manacommunity.api.marketplace.dto.MarketSellerAnalyticsDto;
import com.manacommunity.api.marketplace.entity.MarketAuditLog;
import com.manacommunity.api.marketplace.entity.MarketDispute;
import com.manacommunity.api.marketplace.entity.MarketGroupOrder;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.entity.MarketReport;
import com.manacommunity.api.marketplace.repository.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketAdminService {

    private final MarketListingRepository listingRepository;
    private final MarketOrderRepository orderRepository;
    private final MarketDisputeRepository disputeRepository;
    private final MarketReportRepository reportRepository;
    private final MarketGroupOrderRepository groupOrderRepository;
    private final MarketOfferRepository offerRepository;
    private final MarketReviewRepository reviewRepository;
    private final MarketAuditLogRepository auditLogRepository;

    public MarketSellerAnalyticsDto getSellerAnalytics(Long sellerId) {
        int activeListings = listingRepository.countBySellerIdAndStatus(sellerId, MarketListing.ListingStatus.ACTIVE);
        int soldListings = listingRepository.countBySellerIdAndStatus(sellerId, MarketListing.ListingStatus.SOLD);
        BigDecimal gmv = orderRepository.calculateSellerGmv(sellerId);
        int totalOrders = orderRepository.countBySellerId(sellerId);
        int totalOffers = offerRepository.countBySellerId(sellerId);
        int totalReviews = reviewRepository.countBySellerId(sellerId);
        Double avgRating = reviewRepository.calculateAverageRatingForSeller(sellerId);

        return MarketSellerAnalyticsDto.builder()
                .sellerId(sellerId)
                .activeListings(activeListings)
                .soldListings(soldListings)
                .totalRevenueGmv(gmv)
                .totalOrders(totalOrders)
                .totalOffersReceived(totalOffers)
                .totalReviews(totalReviews)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .responseRatePercent(98.5)
                .build();
    }

    public MarketAdminMetricsDto getAdminMetrics(Long communityId) {
        BigDecimal totalGmv = orderRepository.calculateCommunityGmv(communityId);
        int totalOrders = orderRepository.countByCommunityId(communityId);
        int activeListings = listingRepository.countByCommunityIdAndStatus(communityId, MarketListing.ListingStatus.ACTIVE);
        int openDisputes = disputeRepository.countByCommunityIdAndStatus(communityId, MarketDispute.DisputeStatus.OPEN);
        int pendingReports = reportRepository.countByCommunityIdAndStatus(communityId, MarketReport.ReportStatus.PENDING);
        int activeGroupOrders = groupOrderRepository.countByCommunityIdAndStatus(communityId, MarketGroupOrder.GroupOrderStatus.ACTIVE);

        Map<String, Integer> categoryDistribution = new HashMap<>();
        List<Object[]> catCounts = listingRepository.countListingsByCategory(communityId);
        for (Object[] row : catCounts) {
            if (row != null && row.length >= 2 && row[0] != null) {
                categoryDistribution.put((String) row[0], ((Number) row[1]).intValue());
            }
        }

        return MarketAdminMetricsDto.builder()
                .totalGmv(totalGmv)
                .totalOrders(totalOrders)
                .activeListings(activeListings)
                .openDisputes(openDisputes)
                .pendingReports(pendingReports)
                .activeGroupOrders(activeGroupOrders)
                .categoryDistribution(categoryDistribution)
                .monthlyRevenue(Map.of("Current Month", totalGmv))
                .build();
    }

    public Page<MarketAuditLogDto> getAuditLogs(Long communityId, Pageable pageable) {
        return auditLogRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable)
                .map(this::toAuditDto);
    }

    @Transactional
    public void recordAuditLog(String action, String targetEntity, Long targetId, AppUser actor, Community community, String details, String ip) {
        MarketAuditLog log = MarketAuditLog.builder()
                .action(action)
                .targetEntity(targetEntity)
                .targetId(targetId)
                .actor(actor)
                .community(community)
                .details(details)
                .ipAddress(ip)
                .build();
        auditLogRepository.save(log);
    }

    private MarketAuditLogDto toAuditDto(MarketAuditLog l) {
        return MarketAuditLogDto.builder()
                .id(l.getId())
                .action(l.getAction())
                .targetEntity(l.getTargetEntity())
                .targetId(l.getTargetId())
                .actorId(l.getActor().getId())
                .actorName(l.getActor().getFullName() != null ? l.getActor().getFullName() : l.getActor().getUsername())
                .communityId(l.getCommunity() != null ? l.getCommunity().getId() : null)
                .details(l.getDetails())
                .ipAddress(l.getIpAddress())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
