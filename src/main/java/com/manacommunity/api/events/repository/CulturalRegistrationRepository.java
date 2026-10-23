package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventCulturalRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CulturalRegistrationRepository extends JpaRepository<EventCulturalRegistration, Long> {

    boolean existsByUserIdAndCulturalEventIdAndStatusNot(Long userId, Long culturalEventId, String status);

    long countByCulturalEventIdAndStatusNot(Long culturalEventId, String status);

    List<EventCulturalRegistration> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<EventCulturalRegistration> findByUserIdAndCommunityIdOrderByCreatedAtDesc(Long userId, Long communityId);

    List<EventCulturalRegistration> findByCulturalEventIdOrderByCreatedAtDesc(Long culturalEventId);

    List<EventCulturalRegistration> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<EventCulturalRegistration> findByMainEventIdOrderByCreatedAtDesc(Long mainEventId);
}
