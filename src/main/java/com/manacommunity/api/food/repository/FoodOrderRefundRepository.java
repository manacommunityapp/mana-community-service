package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodOrderRefund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodOrderRefundRepository extends JpaRepository<FoodOrderRefund, Long> {

    Optional<FoodOrderRefund> findByOrderId(Long orderId);

    Page<FoodOrderRefund> findByStatusAndCommunityId(FoodOrderRefund.RefundStatus status, Long communityId, Pageable pageable);
}
