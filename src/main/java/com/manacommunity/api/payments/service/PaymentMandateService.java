package com.manacommunity.api.payments.service;

import com.manacommunity.api.payments.entity.PaymentMandate;
import com.manacommunity.api.payments.repository.PaymentMandateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMandateService {
    private final PaymentMandateRepository repo;

    public List<PaymentMandate> listActive(Long communityId) {
        return repo.findByCommunityIdAndActiveTrue(communityId);
    }

    public List<PaymentMandate> listByUser(Long communityId, Long userId) {
        return repo.findByCommunityIdAndUserId(communityId, userId);
    }

    @Transactional
    public PaymentMandate approve(PaymentMandate mandate) {
        return repo.save(mandate);
    }

    @Transactional
    public PaymentMandate cancel(Long communityId, Long id, String reason) {
        PaymentMandate m = repo.findById(id)
            .filter(x -> x.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Mandate not found"));
        m.setActive(false);
        m.setCancelledAt(Instant.now());
        m.setCancelReason(reason);
        return repo.save(m);
    }
}
