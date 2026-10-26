package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthBenefitUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthBenefitUsageRepository extends JpaRepository<HealthBenefitUsage, Long> {
    Optional<HealthBenefitUsage> findByTenantIdAndResidentUserId(Long tenantId, Long residentUserId);
}