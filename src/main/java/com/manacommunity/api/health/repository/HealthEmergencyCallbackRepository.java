package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthEmergencyCallback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthEmergencyCallbackRepository extends JpaRepository<HealthEmergencyCallback, Long> {
    List<HealthEmergencyCallback> findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(Long tenantId, Long residentUserId);
    Optional<HealthEmergencyCallback> findByCallbackUuid(UUID callbackUuid);
}
