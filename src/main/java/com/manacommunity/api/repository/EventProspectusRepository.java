package com.manacommunity.api.repository;

import com.manacommunity.api.model.EventProspectusConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventProspectusRepository extends JpaRepository<EventProspectusConfig, Long> {

    Optional<EventProspectusConfig> findByEventId(Long eventId);

    Optional<EventProspectusConfig> findFirstByOrderByUpdatedAtDesc();
}
