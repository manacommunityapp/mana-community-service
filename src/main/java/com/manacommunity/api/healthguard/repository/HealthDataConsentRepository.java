package com.manacommunity.api.healthguard.repository;

import com.manacommunity.api.healthguard.entity.HealthDataConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HealthDataConsentRepository extends JpaRepository<HealthDataConsent, Long> {
    Optional<HealthDataConsent> findByUserIdAndProviderAndDataScopeAndConsentVersion(
            Long userId, String provider, String dataScope, String consentVersion);
    List<HealthDataConsent> findByUserIdAndRevokedAtIsNullOrderByProviderAscDataScopeAsc(Long userId);
    List<HealthDataConsent> findByUserIdAndProviderAndRevokedAtIsNull(Long userId, String provider);
    boolean existsByUserIdAndProviderAndDataScopeAndRevokedAtIsNull(
            Long userId, String provider, String dataScope);
}
