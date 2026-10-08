package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceReviewRepository extends JpaRepository<CommerceReview, Long> {
    List<CommerceReview> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId);
    List<CommerceReview> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CommerceReview> findByChannelOrderByCreatedAtDesc(CommerceChannel channel);
    boolean existsByOrderIdAndUserId(Long orderId, Long userId);
}