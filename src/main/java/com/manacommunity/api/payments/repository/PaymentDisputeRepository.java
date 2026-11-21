package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.PaymentDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentDisputeRepository extends JpaRepository<PaymentDispute, Long> {
    List<PaymentDispute> findByCommunityId(Long communityId);
    List<PaymentDispute> findByCommunityIdAndUserId(Long communityId, Long userId);
    List<PaymentDispute> findByCommunityIdAndStatus(Long communityId, String status);
}
