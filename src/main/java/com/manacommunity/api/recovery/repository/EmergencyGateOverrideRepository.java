package com.manacommunity.api.recovery.repository;

import com.manacommunity.api.recovery.entity.EmergencyGateOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmergencyGateOverrideRepository extends JpaRepository<EmergencyGateOverrideEntity, UUID> {

    Optional<EmergencyGateOverrideEntity> findBySocietyIdAndFeatureKey(Long societyId, String featureKey);
}
