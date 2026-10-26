package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.CashfreeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CashfreeOrderRepository extends JpaRepository<CashfreeOrder, Long> {

    Optional<CashfreeOrder> findByManaOrderId(String manaOrderId);

    Optional<CashfreeOrder> findTopByReferenceTypeAndReferenceIdAndStatusOrderByCreatedAtDesc(String referenceType, Long referenceId, String status);

    Optional<CashfreeOrder> findByCfPaymentId(String cfPaymentId);

    boolean existsByWebhookEventId(String webhookEventId);

    Page<CashfreeOrder> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    Page<CashfreeOrder> findByCommunityId(Long communityId, Pageable pageable);

    Page<CashfreeOrder> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId, Pageable pageable);

    // ─── Admin Report Queries ─────────────────────────────────────────────────

    @Query("""
        SELECT o FROM CashfreeOrder o
        WHERE o.community.id = :communityId
          AND (:status       IS NULL OR o.status        = :status)
          AND (:refType      IS NULL OR o.referenceType = :refType)
          AND (:from         IS NULL OR o.createdAt    >= :from)
          AND (:to           IS NULL OR o.createdAt    <= :to)
        ORDER BY o.createdAt DESC
    """)
    Page<CashfreeOrder> findForAdminReport(
            @Param("communityId") Long communityId,
            @Param("status")      String status,
            @Param("refType")     String refType,
            @Param("from")        LocalDateTime from,
            @Param("to")          LocalDateTime to,
            Pageable pageable);

    /** Total amount collected (PAID orders) within a date window. */
    @Query("""
        SELECT COALESCE(SUM(o.amount), 0)
        FROM CashfreeOrder o
        WHERE o.community.id = :communityId
          AND o.status = 'PAID'
          AND (:from IS NULL OR o.paidAt >= :from)
          AND (:to   IS NULL OR o.paidAt <= :to)
    """)
    BigDecimal sumPaidAmount(
            @Param("communityId") Long communityId,
            @Param("from")        LocalDateTime from,
            @Param("to")          LocalDateTime to);

    /** Total amount refunded (REFUNDED orders) within a date window. */
    @Query("""
        SELECT COALESCE(SUM(o.amount), 0)
        FROM CashfreeOrder o
        WHERE o.community.id = :communityId
          AND o.status IN ('REFUNDED', 'REFUND_PENDING')
          AND (:from IS NULL OR o.createdAt >= :from)
          AND (:to   IS NULL OR o.createdAt <= :to)
    """)
    BigDecimal sumRefundedAmount(
            @Param("communityId") Long communityId,
            @Param("from")        LocalDateTime from,
            @Param("to")          LocalDateTime to);

    /** Count of orders per status for a community in a date window. */
    @Query("""
        SELECT o.status, COUNT(o), COALESCE(SUM(o.amount), 0)
        FROM CashfreeOrder o
        WHERE o.community.id = :communityId
          AND (:from IS NULL OR o.createdAt >= :from)
          AND (:to   IS NULL OR o.createdAt <= :to)
        GROUP BY o.status
    """)
    List<Object[]> countAndSumByStatus(
            @Param("communityId") Long communityId,
            @Param("from")        LocalDateTime from,
            @Param("to")          LocalDateTime to);

    /** Count and sum per referenceType for a community in a date window. */
    @Query("""
        SELECT o.referenceType, COUNT(o), COALESCE(SUM(o.amount), 0)
        FROM CashfreeOrder o
        WHERE o.community.id = :communityId
          AND o.status = 'PAID'
          AND (:from IS NULL OR o.paidAt >= :from)
          AND (:to   IS NULL OR o.paidAt <= :to)
        GROUP BY o.referenceType
    """)
    List<Object[]> paidCountAndSumByReferenceType(
            @Param("communityId") Long communityId,
            @Param("from")        LocalDateTime from,
            @Param("to")          LocalDateTime to);
}
