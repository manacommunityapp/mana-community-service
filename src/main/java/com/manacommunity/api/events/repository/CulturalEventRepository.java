package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventCulturalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CulturalEventRepository extends JpaRepository<EventCulturalEvent, Long> {

    List<EventCulturalEvent> findByCommunityIdOrderByDateAscStartTimeAscSortOrderAsc(Long communityId);

    List<EventCulturalEvent> findByMainEventIdOrderByDateAscStartTimeAscSortOrderAsc(Long mainEventId);
}
