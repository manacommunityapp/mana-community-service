package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.CulturalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CulturalEventRepository extends JpaRepository<CulturalEvent, Long> {

    List<CulturalEvent> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<CulturalEvent> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);
}
