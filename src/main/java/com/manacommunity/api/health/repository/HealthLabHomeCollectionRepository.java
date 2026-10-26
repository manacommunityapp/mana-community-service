package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthLabHomeCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthLabHomeCollectionRepository extends JpaRepository<HealthLabHomeCollection, Long> {
    List<HealthLabHomeCollection> findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(Long tenantId, Long residentUserId);
    Optional<HealthLabHomeCollection> findByBookingUuid(UUID bookingUuid);
}
