package com.manacommunity.api.groupbuy.repository;

import com.manacommunity.api.groupbuy.entity.GroupOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GroupOrderRepository extends JpaRepository<GroupOrder, Long> {

    List<GroupOrder> findByUserIdOrderByOrderedAtDesc(Long userId);

    Optional<GroupOrder> findByDealIdAndUserId(Long dealId, Long userId);

    boolean existsByDealIdAndUserId(Long dealId, Long userId);

    @Query("SELECT COUNT(o) FROM GroupOrder o WHERE o.deal.id = :dealId AND o.status <> 'CANCELLED'")
    long countActiveByDeal(Long dealId);

    @Query("SELECT SUM(o.totalAmount) FROM GroupOrder o WHERE o.community.id = :communityId AND o.status = 'DELIVERED'")
    Long sumSavingsByCommunity(Long communityId);
}
