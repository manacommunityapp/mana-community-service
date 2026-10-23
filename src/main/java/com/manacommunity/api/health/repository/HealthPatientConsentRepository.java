package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthPatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthPatientConsentRepository extends JpaRepository<HealthPatientConsent, Long> {
    Optional<HealthPatientConsent> findByPatientIdAndProviderIdAndIsActiveTrue(UUID patientId, Long providerId);
}