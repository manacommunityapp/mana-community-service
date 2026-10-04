package com.manacommunity.api.groupbuying.repository;

import com.manacommunity.api.groupbuying.model.OrderReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderReviewRepository extends JpaRepository<OrderReview, Long> {
    List<OrderReview> findByDealIdOrderByCreatedAtDesc(String dealId);
    List<OrderReview> findByOrderId(String orderId);
    List<OrderReview> findByUserIdOrderByCreatedAtDesc(Long userId);
}
