package com.manacommunity.api.serviceplatform.amc.repository;

import com.manacommunity.api.serviceplatform.amc.entity.AmcSubscription;
import com.manacommunity.api.serviceplatform.amc.entity.AmcSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmcSubscriptionRepository extends JpaRepository<AmcSubscription, Long> {
    List<AmcSubscription> findByUserIdAndStatus(Long userId, AmcSubscriptionStatus status);
    List<AmcSubscription> findByCommunityId(Long communityId);
    List<AmcSubscription> findByAmcPlanId(Long amcPlanId);
}
