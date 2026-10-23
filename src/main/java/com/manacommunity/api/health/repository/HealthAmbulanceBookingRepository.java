package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthAmbulanceBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthAmbulanceBookingRepository extends JpaRepository<HealthAmbulanceBooking, Long> {
    List<HealthAmbulanceBooking> findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(Long tenantId, Long residentUserId);
    Optional<HealthAmbulanceBooking> findByBookingUuid(UUID bookingUuid);
}
