package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.CourtSlotWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourtSlotWaitlistRepository extends JpaRepository<CourtSlotWaitlist, Long> {
    List<CourtSlotWaitlist> findByBookingIdOrderByPosition(Long bookingId);
    List<CourtSlotWaitlist> findByCommunityIdAndUserId(Long communityId, Long userId);
}
