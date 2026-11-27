package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.model.PaymentReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentOrder> findByGatewayAndGatewayOrderId(PaymentGateway gateway, String gatewayOrderId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<PaymentOrder> findByReferenceTypeAndReferenceId(
            PaymentReferenceType referenceType, Long referenceId);

    Page<PaymentOrder> findByCommunityId(Long communityId, Pageable pageable);

    Page<PaymentOrder> findByCommunityIdAndStatus(
            Long communityId, PaymentOrderStatus status, Pageable pageable);

    // Analytics queries

    long countByCommunityIdAndCreatedAtBetween(Long communityId, OffsetDateTime from, OffsetDateTime to);

    long countByCommunityIdAndStatusAndCreatedAtBetween(
            Long communityId, PaymentOrderStatus status, OffsetDateTime from, OffsetDateTime to);

    long countByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);

    long countByStatusAndCreatedAtBetween(PaymentOrderStatus status, OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p " +
           "WHERE p.status = com.manacommunity.api.payments.model.PaymentOrderStatus.SUCCESS " +
           "AND p.createdAt BETWEEN :from AND :to")
    BigDecimal sumSuccessfulAmountBetween(@Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(p.refundedAmount), 0) FROM PaymentOrder p " +
           "WHERE p.updatedAt BETWEEN :from AND :to")
    BigDecimal sumRefundedAmountBetween(@Param("from") OffsetDateTime from,
                                        @Param("to") OffsetDateTime to);

    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.refundedAmount > 0 " +
           "AND p.createdAt BETWEEN :from AND :to")
    long countOrdersWithRefundBetween(@Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to);

    @Query("SELECT p.referenceType, COUNT(p), COALESCE(SUM(p.amount), 0), COUNT(DISTINCT p.payerUserId) FROM PaymentOrder p " +
           "WHERE p.status = com.manacommunity.api.payments.model.PaymentOrderStatus.SUCCESS " +
           "AND p.createdAt BETWEEN :from AND :to GROUP BY p.referenceType")
    List<Object[]> successfulAnalyticsGroupByReferenceType(@Param("from") OffsetDateTime from,
                                                          @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p " +
           "WHERE p.communityId = :communityId AND p.status = :status " +
           "AND p.createdAt BETWEEN :from AND :to")
    BigDecimal sumAmountByCommunityIdAndStatusAndCreatedAtBetween(
            @Param("communityId") Long communityId,
            @Param("status") PaymentOrderStatus status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(p.refundedAmount), 0) FROM PaymentOrder p " +
           "WHERE p.communityId = :communityId AND p.createdAt BETWEEN :from AND :to")
    BigDecimal sumRefundedAmountByCommunityIdAndCreatedAtBetween(
            @Param("communityId") Long communityId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("SELECT p.gateway, COUNT(p), COALESCE(SUM(p.amount), 0), " +
           "SUM(CASE WHEN p.status = 'SUCCESS' THEN 1 ELSE 0 END) " +
           "FROM PaymentOrder p " +
           "WHERE p.communityId = :communityId AND p.createdAt BETWEEN :from AND :to " +
           "GROUP BY p.gateway")
    List<Object[]> analyticsGroupByGateway(
            @Param("communityId") Long communityId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("SELECT p.referenceType, COUNT(p), COALESCE(SUM(p.amount), 0) " +
           "FROM PaymentOrder p " +
           "WHERE p.communityId = :communityId AND p.createdAt BETWEEN :from AND :to " +
           "GROUP BY p.referenceType")
    List<Object[]> analyticsGroupByReferenceType(
            @Param("communityId") Long communityId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query(value =
           "SELECT DATE(created_at) AS day, COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS total " +
           "FROM payment_order " +
           "WHERE community_id = :communityId AND created_at BETWEEN :from AND :to " +
           "GROUP BY day ORDER BY day",
           nativeQuery = true)
    List<Object[]> dailyTrend(
            @Param("communityId") Long communityId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
