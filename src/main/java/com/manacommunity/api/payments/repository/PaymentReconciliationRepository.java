package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.PaymentReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentReconciliationRepository extends JpaRepository<PaymentReconciliation, Long> {
    List<PaymentReconciliation> findByCommunityIdOrderByPeriodStartDesc(Long communityId);
}
