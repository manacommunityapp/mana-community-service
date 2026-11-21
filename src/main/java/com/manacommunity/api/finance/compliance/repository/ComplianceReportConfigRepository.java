package com.manacommunity.api.finance.compliance.repository;

import com.manacommunity.api.finance.compliance.entity.ComplianceReportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ComplianceReportConfigRepository extends JpaRepository<ComplianceReportConfig, Long> {
    List<ComplianceReportConfig> findByCommunityIdAndIsActiveTrue(Long communityId);
    Optional<ComplianceReportConfig> findByCommunityIdAndReportType(Long communityId, String reportType);
    List<ComplianceReportConfig> findByIsActiveTrue();
}
