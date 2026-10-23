package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthProvider;
import com.manacommunity.api.health.enums.HealthEnums.ProviderCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthProviderRepository extends JpaRepository<HealthProvider, Long> {
    List<HealthProvider> findByTenantIdAndIsActiveTrue(Long tenantId);
    List<HealthProvider> findByTenantIdAndCategoryAndIsActiveTrue(Long tenantId, ProviderCategory category);
    Optional<HealthProvider> findByProviderUuid(UUID providerUuid);
}