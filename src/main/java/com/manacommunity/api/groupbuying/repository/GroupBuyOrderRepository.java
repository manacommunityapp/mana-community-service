package com.manacommunity.api.groupbuying.repository;

import com.manacommunity.api.groupbuying.model.GroupBuyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupBuyOrderRepository extends JpaRepository<GroupBuyOrder, Long> {
    List<GroupBuyOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<GroupBuyOrder> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<GroupBuyOrder> findByDealId(Long dealId);
    Optional<GroupBuyOrder> findByQrToken(String qrToken);
    Optional<GroupBuyOrder> findByOrderNumber(String orderNumber);
}
