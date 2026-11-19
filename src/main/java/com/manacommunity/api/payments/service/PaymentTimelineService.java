package com.manacommunity.api.payments.service;

import com.manacommunity.api.payments.entity.PaymentTimelineEvent;
import com.manacommunity.api.payments.repository.PaymentTimelineEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentTimelineService {
    private final PaymentTimelineEventRepository repo;

    public List<PaymentTimelineEvent> listForUser(Long communityId, Long userId, int limit) {
        return repo.findByCommunityIdAndUserIdOrderByOccurredAtDesc(communityId, userId, PageRequest.of(0, limit));
    }

    public List<PaymentTimelineEvent> listForCommunity(Long communityId, int limit) {
        return repo.findByCommunityIdOrderByOccurredAtDesc(communityId, PageRequest.of(0, limit));
    }

    @Transactional
    public PaymentTimelineEvent record(PaymentTimelineEvent event) {
        return repo.save(event);
    }
}
