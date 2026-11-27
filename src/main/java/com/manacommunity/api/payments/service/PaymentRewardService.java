package com.manacommunity.api.payments.service;

import com.manacommunity.api.payments.entity.PaymentReward;
import com.manacommunity.api.payments.repository.PaymentRewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentRewardService {
    private final PaymentRewardRepository repo;

    public List<PaymentReward> listActive(Long communityId, Long userId) {
        return repo.findByCommunityIdAndUserIdAndRedeemedFalse(communityId, userId);
    }

    public List<PaymentReward> listAll(Long communityId, Long userId) {
        return repo.findByCommunityIdAndUserId(communityId, userId);
    }

    public Map<String, Object> balance(Long communityId, Long userId) {
        return Map.of("userId", userId, "points", repo.totalActivePoints(communityId, userId));
    }

    @Transactional
    public PaymentReward grant(PaymentReward reward) {
        return repo.save(reward);
    }

    @Transactional
    public PaymentReward redeem(Long communityId, Long userId, Long id) {
        PaymentReward r = repo.findById(id)
            .filter(x -> x.getCommunityId().equals(communityId) && x.getUserId().equals(userId) && !x.getRedeemed())
            .orElseThrow(() -> new IllegalArgumentException("Reward not found or already redeemed"));
        r.setRedeemed(true);
        r.setRedeemedAt(Instant.now());
        return repo.save(r);
    }
}
