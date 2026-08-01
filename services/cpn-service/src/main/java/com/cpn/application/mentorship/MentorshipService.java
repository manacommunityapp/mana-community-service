package com.cpn.application.mentorship;

import com.cpn.domain.mentorship.model.MentorBooking;
import com.cpn.domain.mentorship.repository.MentorshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MentorshipService {

    private final MentorshipRepository mentorshipRepository;

    @Transactional
    public MentorBooking bookSession(MentorBooking booking) {
        booking.setStatus("REQUESTED");
        return mentorshipRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public List<MentorBooking> getMenteeBookings(UUID menteeId) {
        return mentorshipRepository.findByMenteeId(menteeId);
    }
}
