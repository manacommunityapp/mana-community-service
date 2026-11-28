package com.manacommunity.api.release.repository;

import com.manacommunity.api.release.entity.TenantFeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantFeatureFlagRepository extends JpaRepository<TenantFeatureFlag, Long> {
    Optional<TenantFeatureFlag> findByFlagKey(String flagKey);
}
