package com.manacommunity.api.delivery.repository;

import com.manacommunity.api.delivery.model.ResidentDeliveryPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResidentDeliveryPreferenceRepository extends JpaRepository<ResidentDeliveryPreference, Long> {
    Optional<ResidentDeliveryPreference> findByCommunityIdAndFlatNumber(Long communityId, String flatNumber);
    Optional<ResidentDeliveryPreference> findByUserId(Long userId);
}
