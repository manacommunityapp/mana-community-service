package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.CashfreeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashfreeOrderRepository extends JpaRepository<CashfreeOrder, Long> {

    Optional<CashfreeOrder> findByManaOrderId(String manaOrderId);

    Optional<CashfreeOrder> findByCfPaymentId(String cfPaymentId);

    boolean existsByWebhookEventId(String webhookEventId);

    Page<CashfreeOrder> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    Page<CashfreeOrder> findByCommunityId(Long communityId, Pageable pageable);

    Page<CashfreeOrder> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId, Pageable pageable);
}
