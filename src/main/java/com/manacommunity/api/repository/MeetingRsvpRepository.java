package com.manacommunity.api.repository;

import com.manacommunity.api.model.MeetingRsvp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRsvpRepository extends JpaRepository<MeetingRsvp, Long> {
    Optional<MeetingRsvp> findByMeetingIdAndUserId(Long meetingId, Long userId);
    long countByMeetingId(Long meetingId);
}
