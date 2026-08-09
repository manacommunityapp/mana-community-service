package com.manacommunity.api.repository;

import com.manacommunity.api.model.EventVenueConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventVenueConfigRepository extends JpaRepository<EventVenueConfig, Long> {

    Optional<EventVenueConfig> findTopByEventIdOrderByUpdatedAtDesc(Long eventId);

    Optional<EventVenueConfig> findTopByCommunityIdOrderByUpdatedAtDesc(Long communityId);

    Optional<EventVenueConfig> findTopByOrderByUpdatedAtDesc();
}
