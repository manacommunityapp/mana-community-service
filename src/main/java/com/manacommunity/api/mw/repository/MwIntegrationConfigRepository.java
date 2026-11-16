package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwIntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MwIntegrationConfigRepository extends JpaRepository<MwIntegrationConfig, Long> {
    Optional<MwIntegrationConfig> findByOrganizationId(Long orgId);
}
