package com.manacommunity.api.payments.service;

import com.manacommunity.api.payments.entity.PaymentReceipt;
import com.manacommunity.api.payments.repository.PaymentReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentReceiptService {
    private final PaymentReceiptRepository repo;

    public List<PaymentReceipt> listByUser(Long communityId, Long userId) {
        return repo.findByCommunityIdAndUserId(communityId, userId);
    }

    public List<PaymentReceipt> listAll(Long communityId) {
        return repo.findByCommunityId(communityId);
    }

    @Transactional
    public PaymentReceipt issue(PaymentReceipt receipt) {
        if (receipt.getReceiptNumber() == null || receipt.getReceiptNumber().isBlank()) {
            receipt.setReceiptNumber("RCP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        if (receipt.getPaidAt() == null) receipt.setPaidAt(Instant.now());
        return repo.save(receipt);
    }
}
