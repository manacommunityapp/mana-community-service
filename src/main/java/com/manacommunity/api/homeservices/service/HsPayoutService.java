package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.entity.HsWorkerPayout;
import com.manacommunity.api.homeservices.repository.HsWorkerPayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HsPayoutService {

    private final HsWorkerPayoutRepository payoutRepo;

    @Transactional
    public HsWorkerPayout create(HsWorkerPayout payout) {
        payout.setNetAmount(payout.getAmount().subtract(payout.getDeductions()));
        return payoutRepo.save(payout);
    }

    public List<HsWorkerPayout> workerStatement(Long workerId) {
        return payoutRepo.findByWorkerIdOrderByCreatedAtDesc(workerId);
    }

    public List<HsWorkerPayout> unpaidPayouts(Long communityId) {
        return payoutRepo.findByCommunityIdAndPaidFalse(communityId);
    }

    @Transactional
    public HsWorkerPayout markPaid(Long payoutId) {
        HsWorkerPayout p = payoutRepo.findById(payoutId)
            .orElseThrow(() -> new IllegalArgumentException("Payout not found"));
        p.setPaid(true);
        p.setPaidAt(Instant.now());
        return payoutRepo.save(p);
    }
}
