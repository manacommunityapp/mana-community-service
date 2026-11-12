package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrReportConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrReportConfigRepository extends JpaRepository<HrReportConfig, Long> {

    List<HrReportConfig> findByOrganizationIdOrderByCreatedAtDesc(Long orgId);
}
