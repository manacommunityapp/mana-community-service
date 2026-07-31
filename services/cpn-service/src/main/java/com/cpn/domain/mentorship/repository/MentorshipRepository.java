package com.cpn.domain.mentorship.repository;

import com.cpn.domain.mentorship.model.MentorBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MentorshipRepository extends JpaRepository<MentorBooking, UUID> {
    List<MentorBooking> findByMentorId(UUID mentorId);
    List<MentorBooking> findByMenteeId(UUID menteeId);
}
