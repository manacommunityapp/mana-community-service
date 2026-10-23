package com.manacommunity.api.repository;

import com.manacommunity.api.sports.model.SportsEventVenueConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SportsEventVenueConfigRepository extends JpaRepository<SportsEventVenueConfig, Long> {

    Optional<SportsEventVenueConfig> findTopByEventIdOrderByUpdatedAtDesc(Long eventId);

    Optional<SportsEventVenueConfig> findTopByCommunityIdOrderByUpdatedAtDesc(Long communityId);

    Optional<SportsEventVenueConfig> findTopByOrderByUpdatedAtDesc();
}
