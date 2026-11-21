package com.manacommunity.api.social.repository;

import com.manacommunity.api.social.entity.GroupEventRsvp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupEventRsvpRepository extends JpaRepository<GroupEventRsvp, Long> {
    Optional<GroupEventRsvp> findByEventIdAndUserId(Long eventId, Long userId);
}
