package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityJourneyMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MobilityJourneyMatchRepository extends JpaRepository<MobilityJourneyMatch, Long> {
    Optional<MobilityJourneyMatch> findByRequestIdAndOfferId(Long requestId, Long offerId);
    Page<MobilityJourneyMatch> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);
    Optional<MobilityJourneyMatch> findByIdAndRequesterId(Long id, Long requesterId);
}
