package com.manacommunity.api.transactioncore.repository;

import com.manacommunity.api.transactioncore.entity.TransactionIntent;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionIntentRepository extends JpaRepository<TransactionIntent, Long> {
    Optional<TransactionIntent> findByIntentId(String intentId);
    Optional<TransactionIntent> findByIdempotencyKey(String idempotencyKey);
    Optional<TransactionIntent> findByReceiptNumber(String receiptNumber);
    Optional<TransactionIntent> findByGatewayOrderId(String gatewayOrderId);
    Optional<TransactionIntent> findByOrderReferenceId(String orderReferenceId);
    Optional<TransactionIntent> findByDomainAndOrderReferenceId(TransactionDomain domain, String orderReferenceId);
    Page<TransactionIntent> findByPayerId(Long payerId, Pageable pageable);
    Page<TransactionIntent> findByCommunityIdAndStatus(Long communityId, TransactionIntentStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionIntent t WHERE t.communityId = :communityId AND t.status IN :statuses")
    BigDecimal sumAmountByCommunityIdAndStatusIn(@Param("communityId") Long communityId, @Param("statuses") List<TransactionIntentStatus> statuses);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionIntent t WHERE t.communityId = :communityId AND t.domain = :domain AND t.status IN :statuses")
    BigDecimal sumAmountByCommunityIdAndDomainAndStatusIn(@Param("communityId") Long communityId, @Param("domain") TransactionDomain domain, @Param("statuses") List<TransactionIntentStatus> statuses);

    @Query("SELECT COUNT(t) FROM TransactionIntent t WHERE t.communityId = :communityId AND t.escrowHeld = true AND t.status = :status")
    long countEscrowHeldByCommunityIdAndStatus(@Param("communityId") Long communityId, @Param("status") TransactionIntentStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionIntent t WHERE t.communityId = :communityId AND t.escrowHeld = true AND t.status = :status")
    BigDecimal sumEscrowHeldByCommunityIdAndStatus(@Param("communityId") Long communityId, @Param("status") TransactionIntentStatus status);

    long countByCommunityId(Long communityId);
}
