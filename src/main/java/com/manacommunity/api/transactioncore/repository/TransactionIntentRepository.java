package com.manacommunity.api.transactioncore.repository;

import com.manacommunity.api.transactioncore.entity.TransactionIntent;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionIntentRepository extends JpaRepository<TransactionIntent, Long> {
    Optional<TransactionIntent> findByIntentId(String intentId);
    Optional<TransactionIntent> findByIdempotencyKey(String idempotencyKey);
    Optional<TransactionIntent> findByDomainAndOrderReferenceId(TransactionDomain domain, String orderReferenceId);
    Page<TransactionIntent> findByPayerId(Long payerId, Pageable pageable);
    Page<TransactionIntent> findByCommunityIdAndStatus(Long communityId, TransactionIntentStatus status, Pageable pageable);
}
