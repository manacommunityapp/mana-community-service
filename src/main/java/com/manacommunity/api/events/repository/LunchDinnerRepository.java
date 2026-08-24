package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventLunchDinner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LunchDinnerRepository extends JpaRepository<EventLunchDinner, Long> {

    List<EventLunchDinner> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<EventLunchDinner> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);
}
