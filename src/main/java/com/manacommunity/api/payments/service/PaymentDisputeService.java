package com.manacommunity.api.payments.service;

import com.manacommunity.api.payments.entity.PaymentDispute;
import com.manacommunity.api.payments.repository.PaymentDisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentDisputeService {
    private final PaymentDisputeRepository repo;

    public List<PaymentDispute> listAll(Long communityId) {
        return repo.findByCommunityId(communityId);
    }

    public List<PaymentDispute> listByUser(Long communityId, Long userId) {
        return repo.findByCommunityIdAndUserId(communityId, userId);
    }

    public List<PaymentDispute> listOpen(Long communityId) {
        return repo.findByCommunityIdAndStatus(communityId, "OPEN");
    }

    @Transactional
    public PaymentDispute raise(PaymentDispute dispute) {
        return repo.save(dispute);
    }

    @Transactional
    public PaymentDispute resolve(Long communityId, Long id, String resolution) {
        PaymentDispute d = repo.findById(id)
            .filter(x -> x.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        d.setStatus("RESOLVED");
        d.setResolution(resolution);
        d.setResolvedAt(Instant.now());
        return repo.save(d);
    }
}
