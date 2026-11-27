package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.PaymentReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PaymentRewardRepository extends JpaRepository<PaymentReward, Long> {
    List<PaymentReward> findByCommunityIdAndUserId(Long communityId, Long userId);
    List<PaymentReward> findByCommunityIdAndUserIdAndRedeemedFalse(Long communityId, Long userId);
    @Query("SELECT COALESCE(SUM(r.points),0) FROM PaymentReward r WHERE r.communityId = :communityId AND r.userId = :userId AND r.redeemed = false")
    Integer totalActivePoints(Long communityId, Long userId);
}
