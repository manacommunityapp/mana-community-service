package com.manacommunity.api.payments.service;

import com.manacommunity.api.payments.entity.PaymentReconciliation;
import com.manacommunity.api.payments.repository.PaymentReconciliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {
    private final PaymentReconciliationRepository repo;

    public List<PaymentReconciliation> list(Long communityId) {
        return repo.findByCommunityIdOrderByPeriodStartDesc(communityId);
    }

    @Transactional
    public PaymentReconciliation create(PaymentReconciliation recon) {
        return repo.save(recon);
    }

    @Transactional
    public PaymentReconciliation approve(Long communityId, Long id, Long reviewedBy) {
        PaymentReconciliation r = repo.findById(id)
            .filter(x -> x.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Reconciliation not found"));
        r.setStatus("APPROVED");
        r.setReviewedBy(reviewedBy);
        r.setReviewedAt(Instant.now());
        return repo.save(r);
    }
}
