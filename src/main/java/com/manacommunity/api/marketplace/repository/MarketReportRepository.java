package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketReportRepository extends JpaRepository<MarketReport, Long> {
    Page<MarketReport> findByCommunityIdAndStatus(Long communityId, MarketReport.ReportStatus status, Pageable pageable);
    List<MarketReport> findByListingId(Long listingId);
    int countByCommunityIdAndStatus(Long communityId, MarketReport.ReportStatus status);
}
