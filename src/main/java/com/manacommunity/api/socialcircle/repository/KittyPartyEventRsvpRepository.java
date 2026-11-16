package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyEventRsvp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyEventRsvpRepository extends JpaRepository<KittyPartyEventRsvp, Long> {

    List<KittyPartyEventRsvp> findByEventId(Long eventId);

    Optional<KittyPartyEventRsvp> findByEventIdAndUserId(Long eventId, Long userId);

    long countByEventIdAndResponse(Long eventId, String response);

    List<KittyPartyEventRsvp> findByUserId(Long userId);
}
