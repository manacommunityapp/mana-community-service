package com.manacommunity.api.healthguard.repository;

import com.manacommunity.api.healthguard.entity.CommunityResponder;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.ResponderAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunityResponderRepository extends JpaRepository<CommunityResponder, Long> {
    Optional<CommunityResponder> findByUserId(Long userId);
    List<CommunityResponder> findByIsActiveTrueAndAvailabilityStatus(ResponderAvailability status);
    long countByIsActiveTrueAndAvailabilityStatus(ResponderAvailability status);
}
