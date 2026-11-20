package com.manacommunity.api.finance.compliance.repository;

import com.manacommunity.api.finance.compliance.entity.GeneratedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, Long> {
    List<GeneratedReport> findByCommunityIdOrderByGeneratedAtDesc(Long communityId);
    List<GeneratedReport> findByCommunityIdAndReportTypeOrderByGeneratedAtDesc(Long communityId, String reportType);
    List<GeneratedReport> findByCommunityIdAndReportTypeAndReportPeriodOrderByGeneratedAtDesc(
            Long communityId, String reportType, String reportPeriod);
}
