package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthIpdAdmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthIpdAdmissionRepository extends JpaRepository<HealthIpdAdmission, Long> {
    List<HealthIpdAdmission> findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(Long tenantId, Long residentUserId);
    Optional<HealthIpdAdmission> findByAdmissionUuid(UUID admissionUuid);
}
