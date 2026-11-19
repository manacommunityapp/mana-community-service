package com.manacommunity.api.sportsops.service;

import com.manacommunity.api.sportsops.entity.SportsNoShowDeposit;
import com.manacommunity.api.sportsops.repository.SportsNoShowDepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepositService {
    private final SportsNoShowDepositRepository repo;

    public List<SportsNoShowDeposit> listByCommunity(Long communityId) {
        return repo.findByCommunityId(communityId);
    }

    public List<SportsNoShowDeposit> listByUser(Long communityId, Long userId) {
        return repo.findByCommunityIdAndUserId(communityId, userId);
    }

    @Transactional
    public SportsNoShowDeposit hold(SportsNoShowDeposit deposit) {
        return repo.save(deposit);
    }

    @Transactional
    public SportsNoShowDeposit release(Long communityId, Long id, String reason) {
        SportsNoShowDeposit d = repo.findById(id)
            .filter(x -> x.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Deposit not found"));
        d.setReleased(true);
        d.setReleasedAt(Instant.now());
        d.setReason(reason);
        return repo.save(d);
    }

    @Transactional
    public SportsNoShowDeposit forfeit(Long communityId, Long id, String reason) {
        SportsNoShowDeposit d = repo.findById(id)
            .filter(x -> x.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Deposit not found"));
        d.setForfeited(true);
        d.setForfeitedAt(Instant.now());
        d.setReason(reason);
        return repo.save(d);
    }
}
