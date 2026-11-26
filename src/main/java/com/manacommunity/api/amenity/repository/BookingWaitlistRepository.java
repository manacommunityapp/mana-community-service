package com.manacommunity.api.amenity.repository;

import com.manacommunity.api.amenity.entity.BookingWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingWaitlistRepository extends JpaRepository<BookingWaitlist, Long> {

    List<BookingWaitlist> findBySlotIdAndStatusOrderByPositionAsc(Long slotId, String status);

    Optional<BookingWaitlist> findBySlotIdAndResidentId(Long slotId, Long residentId);

    @Query("SELECT w FROM BookingWaitlist w WHERE w.status = 'OFFERED' AND w.offerExpiresAt < :now")
    List<BookingWaitlist> findExpiredOffers(@Param("now") Instant now);

    @Query("SELECT MAX(w.position) FROM BookingWaitlist w WHERE w.slotId = :slotId")
    Optional<Integer> findMaxPosition(@Param("slotId") Long slotId);

    long countBySlotIdAndStatus(Long slotId, String status);

    List<BookingWaitlist> findByResidentIdAndStatusIn(Long residentId, List<String> statuses);
}
