package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.CourtSlotSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourtSlotSubscriptionRepository extends JpaRepository<CourtSlotSubscription, Long> {
    List<CourtSlotSubscription> findByCommunityIdAndActiveTrue(Long communityId);
    List<CourtSlotSubscription> findByCommunityIdAndUserId(Long communityId, Long userId);
}
