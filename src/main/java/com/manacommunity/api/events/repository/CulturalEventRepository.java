package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventCulturalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CulturalEventRepository extends JpaRepository<EventCulturalEvent, Long> {

    List<EventCulturalEvent> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<EventCulturalEvent> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);
}
