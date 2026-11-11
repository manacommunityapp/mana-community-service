package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwForecastConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MwForecastConfigRepository extends JpaRepository<MwForecastConfig, Long> {
    Optional<MwForecastConfig> findByOrganizationId(Long orgId);
}
