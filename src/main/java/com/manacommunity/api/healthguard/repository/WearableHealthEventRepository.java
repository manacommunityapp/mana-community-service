package com.manacommunity.api.healthguard.repository;

import com.manacommunity.api.healthguard.entity.WearableHealthEvent;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.WearableEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WearableHealthEventRepository extends JpaRepository<WearableHealthEvent, Long> {
    List<WearableHealthEvent> findByUserIdOrderByEventAtDesc(Long userId);
    List<WearableHealthEvent> findByUserIdAndEventAtAfterOrderByEventAtDesc(Long userId, LocalDateTime since);
    List<WearableHealthEvent> findByUserIdAndEventTypeOrderByEventAtDesc(Long userId, WearableEventType eventType);
    List<WearableHealthEvent> findAllByEventTypeAndEventAtAfterOrderByEventAtDesc(WearableEventType eventType, LocalDateTime since);
    List<WearableHealthEvent> findAllByEventAtAfterOrderByEventAtDesc(LocalDateTime since);
}
