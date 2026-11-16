package com.manacommunity.api.wellnessbasket.repository;

import com.manacommunity.api.wellnessbasket.entity.WellnessSubscriberPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WellnessSubscriberPreferenceRepository extends JpaRepository<WellnessSubscriberPreference, Long> {

    Optional<WellnessSubscriberPreference> findByUserId(Long userId);

    List<WellnessSubscriberPreference> findBySubscriptionStatus(String subscriptionStatus);

    List<WellnessSubscriberPreference> findByClusterIdAndSubscriptionStatus(Long clusterId, String subscriptionStatus);
}
