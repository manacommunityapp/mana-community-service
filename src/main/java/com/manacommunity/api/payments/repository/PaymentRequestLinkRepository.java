package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.PaymentRequestLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRequestLinkRepository extends JpaRepository<PaymentRequestLink, Long> {
    List<PaymentRequestLink> findByCommunityIdAndCreatedBy(Long communityId, Long createdBy);
    Optional<PaymentRequestLink> findByLinkToken(String linkToken);
}
