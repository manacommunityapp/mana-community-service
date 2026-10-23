package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceReviewRepository extends JpaRepository<CommerceReview, Long> {
    List<CommerceReview> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId);
    List<CommerceReview> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CommerceReview> findByChannelOrderByCreatedAtDesc(CommerceChannel channel);
    boolean existsByOrderIdAndUserId(Long orderId, Long userId);

    // Phase 14: aggregate rating for a vendor/seller target
    @Query("SELECT AVG(r.rating), COUNT(r), AVG(r.qualityScore), AVG(r.onTimeScore) FROM CommerceReview r WHERE r.targetType = :targetType AND r.targetId = :targetId")
    Object[] aggregateRating(@Param("targetType") String targetType, @Param("targetId") String targetId);

    @Query("SELECT r.targetId, AVG(r.rating), COUNT(r) FROM CommerceReview r WHERE r.order.community.id = :communityId AND r.targetType = 'VENDOR' GROUP BY r.targetId ORDER BY AVG(r.rating) DESC")
    List<Object[]> vendorRatingSummaryByCommunity(@Param("communityId") Long communityId);
}