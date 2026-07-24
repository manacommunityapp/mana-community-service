package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodPaymentRepository extends JpaRepository<FoodPayment, Long> {

    List<FoodPayment> findByOrderTypeAndOrderId(String orderType, Long orderId);

    Page<FoodPayment> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);
}
