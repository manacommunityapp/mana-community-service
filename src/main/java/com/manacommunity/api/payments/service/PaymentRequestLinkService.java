package com.manacommunity.api.payments.service;

import com.manacommunity.api.payments.entity.PaymentRequestLink;
import com.manacommunity.api.payments.repository.PaymentRequestLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentRequestLinkService {
    private final PaymentRequestLinkRepository repo;

    public List<PaymentRequestLink> listCreatedBy(Long communityId, Long userId) {
        return repo.findByCommunityIdAndCreatedBy(communityId, userId);
    }

    @Transactional
    public PaymentRequestLink create(PaymentRequestLink link) {
        link.setLinkToken(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        return repo.save(link);
    }

    @Transactional
    public PaymentRequestLink markPaid(Long communityId, Long id, Long paidBy) {
        PaymentRequestLink link = repo.findById(id)
            .filter(x -> x.getCommunityId().equals(communityId) && !x.getPaid())
            .orElseThrow(() -> new IllegalArgumentException("Link not found or already paid"));
        link.setPaid(true);
        link.setPaidAt(Instant.now());
        link.setPaidBy(paidBy);
        return repo.save(link);
    }
}
