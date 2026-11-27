package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.PaymentMandate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentMandateRepository extends JpaRepository<PaymentMandate, Long> {
    List<PaymentMandate> findByCommunityIdAndUserId(Long communityId, Long userId);
    List<PaymentMandate> findByCommunityIdAndActiveTrue(Long communityId);
}
