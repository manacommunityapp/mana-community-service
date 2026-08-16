package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.LunchDinner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LunchDinnerRepository extends JpaRepository<LunchDinner, Long> {

    List<LunchDinner> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<LunchDinner> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);
}
