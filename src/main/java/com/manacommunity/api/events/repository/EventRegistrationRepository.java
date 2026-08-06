package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    List<EventRegistration> findByEventId(Long eventId);

    List<EventRegistration> findByUserIdOrderByRegisteredAtDesc(Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    long countByEventCommunityId(Long communityId);
}
