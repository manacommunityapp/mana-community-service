package com.manacommunity.api.sportsops.service;

import com.manacommunity.api.sportsops.entity.CourtSlotWaitlist;
import com.manacommunity.api.sportsops.repository.CourtSlotWaitlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaitlistService {
    private final CourtSlotWaitlistRepository repo;

    public List<CourtSlotWaitlist> listByBooking(Long bookingId) {
        return repo.findByBookingIdOrderByPosition(bookingId);
    }

    public List<CourtSlotWaitlist> listByUser(Long communityId, Long userId) {
        return repo.findByCommunityIdAndUserId(communityId, userId);
    }

    @Transactional
    public CourtSlotWaitlist add(CourtSlotWaitlist entry) {
        List<CourtSlotWaitlist> existing = repo.findByBookingIdOrderByPosition(entry.getBookingId());
        entry.setPosition(existing.size() + 1);
        return repo.save(entry);
    }

    @Transactional
    public CourtSlotWaitlist markNotified(Long communityId, Long id) {
        CourtSlotWaitlist entry = repo.findById(id)
            .filter(e -> e.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Waitlist entry not found"));
        entry.setNotified(true);
        entry.setNotifiedAt(java.time.Instant.now());
        return repo.save(entry);
    }
}
