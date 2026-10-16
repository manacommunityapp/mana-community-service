package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthCareJourney;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthCareJourneyRepository extends JpaRepository<HealthCareJourney, Long> {
    List<HealthCareJourney> findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(Long tenantId, Long residentUserId);
    Optional<HealthCareJourney> findByCareJourneyUuid(UUID careJourneyUuid);
}
