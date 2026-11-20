package com.manacommunity.api.sportsops.service;

import com.manacommunity.api.sportsops.entity.CourtSlotSubscription;
import com.manacommunity.api.sportsops.repository.CourtSlotSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourtSubscriptionService {
    private final CourtSlotSubscriptionRepository repo;

    public List<CourtSlotSubscription> list(Long communityId) {
        return repo.findByCommunityIdAndActiveTrue(communityId);
    }

    public List<CourtSlotSubscription> listByUser(Long communityId, Long userId) {
        return repo.findByCommunityIdAndUserId(communityId, userId);
    }

    @Transactional
    public CourtSlotSubscription create(CourtSlotSubscription sub) {
        return repo.save(sub);
    }

    @Transactional
    public CourtSlotSubscription cancel(Long communityId, Long id) {
        CourtSlotSubscription sub = repo.findById(id)
            .filter(s -> s.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        sub.setActive(false);
        return repo.save(sub);
    }
}
