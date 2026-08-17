package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventVenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventVenueRepository extends JpaRepository<EventVenue, Long> {
    List<EventVenue> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<EventVenue> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
}