package com.manacommunity.api.healthguard.repository;

import com.manacommunity.api.healthguard.entity.HealthObservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface HealthObservationRepository extends JpaRepository<HealthObservation, Long> {
    Optional<HealthObservation> findByUserIdAndProviderAndSourceRecordId(Long userId, String provider, String sourceRecordId);
    List<HealthObservation> findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
            Long userId, OffsetDateTime from, OffsetDateTime to);

    @Transactional
    long deleteByUserIdAndObservationType(Long userId, String observationType);
}
