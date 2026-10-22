package com.manacommunity.api.healthguard.repository;

import com.manacommunity.api.healthguard.entity.EmergencyIncident;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyIncidentRepository extends JpaRepository<EmergencyIncident, Long> {
    Optional<EmergencyIncident> findByUserIdAndStatus(Long userId, IncidentStatus status);
    List<EmergencyIncident> findByUserIdOrderByTriggeredAtDesc(Long userId);
    Optional<EmergencyIncident> findByIncidentUuid(String incidentUuid);
    Optional<EmergencyIncident> findByIdAndUserId(Long id, Long userId);
}
