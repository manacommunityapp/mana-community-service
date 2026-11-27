package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.OrderRefund;
import com.manacommunity.api.food.entity.OrderRefund.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRefundRepository extends JpaRepository<OrderRefund, Long> {
    List<OrderRefund> findByCommunityIdAndStatus(Long communityId, RefundStatus status);
    List<OrderRefund> findByCommunityId(Long communityId);
}
