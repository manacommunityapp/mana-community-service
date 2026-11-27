package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.PaymentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {
    List<PaymentReceipt> findByCommunityIdAndUserId(Long communityId, Long userId);
    List<PaymentReceipt> findByCommunityId(Long communityId);
    Optional<PaymentReceipt> findByReceiptNumber(String receiptNumber);
}
