package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventSponsor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventSponsorRepository extends JpaRepository<EventSponsor, Long> {
    List<EventSponsor> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<EventSponsor> findByEventCommunityIdOrderByCreatedAtDesc(Long communityId);
}
