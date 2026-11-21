package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.PaymentTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PaymentTimelineEventRepository extends JpaRepository<PaymentTimelineEvent, Long> {
    List<PaymentTimelineEvent> findByCommunityIdAndUserIdOrderByOccurredAtDesc(Long communityId, Long userId, Pageable pageable);
    List<PaymentTimelineEvent> findByCommunityIdOrderByOccurredAtDesc(Long communityId, Pageable pageable);
}
