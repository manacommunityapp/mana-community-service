package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthPatientRepository extends JpaRepository<HealthPatient, Long> {
    List<HealthPatient> findByTenantIdAndResidentUserIdAndIsActiveTrue(Long tenantId, Long residentUserId);
    Optional<HealthPatient> findByPatientUuid(UUID patientUuid);
}