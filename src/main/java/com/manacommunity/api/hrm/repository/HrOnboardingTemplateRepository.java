package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrOnboardingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrOnboardingTemplateRepository extends JpaRepository<HrOnboardingTemplate, Long> {
    List<HrOnboardingTemplate> findByOrganizationIdAndIsActiveTrue(Long organizationId);
}
