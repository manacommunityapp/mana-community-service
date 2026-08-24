package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.RazorpayOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface RazorpayOrderRepository extends JpaRepository<RazorpayOrder, Long> {

    Optional<RazorpayOrder> findByRazorpayOrderId(String razorpayOrderId);

    Optional<RazorpayOrder> findByRazorpayPaymentId(String razorpayPaymentId);

    /** Used for webhook idempotency — skip if we've already processed this Razorpay event. */
    boolean existsByWebhookEventId(String webhookEventId);

    /** Member's own payment history within their community, newest first. */
    Page<RazorpayOrder> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    /** All orders for a community — for admin dashboards, newest first. */
    Page<RazorpayOrder> findByCommunityId(Long communityId, Pageable pageable);

    /** All orders for a specific business entity (e.g., all payments for event #42). */
    Page<RazorpayOrder> findByReferenceTypeAndReferenceId(
            String referenceType, Long referenceId, Pageable pageable);

    /** Aggregate collection for finance reporting. */
    @Query("""
        SELECT COALESCE(SUM(o.amount), 0)
        FROM RazorpayOrder o
        WHERE o.community.id = :communityId
          AND o.status = 'PAID'
          AND (:referenceType IS NULL OR o.referenceType = :referenceType)
        """)
    BigDecimal sumPaidByCommunity(
            @Param("communityId") Long communityId,
            @Param("referenceType") String referenceType);
}
