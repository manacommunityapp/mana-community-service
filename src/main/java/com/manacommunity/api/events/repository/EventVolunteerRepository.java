package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventVolunteer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventVolunteerRepository extends JpaRepository<EventVolunteer, Long> {


    List<EventVolunteer> findByEventIdOrderByCreatedAtDesc(Long eventId);

    List<EventVolunteer> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    long countByEventId(Long eventId);

    long countByCommunityId(Long communityId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    List<EventVolunteer> findByEventCommunityIdOrderByCreatedAtDesc(Long communityId);

}
