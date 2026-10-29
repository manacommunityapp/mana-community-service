package com.manacommunity.api.homeservices.instahelp.repository;

import com.manacommunity.api.homeservices.instahelp.model.InstaHelpBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstaHelpBookingRepository extends JpaRepository<InstaHelpBooking, Long> {

    List<InstaHelpBooking> findByResidentIdOrderByBookedAtDesc(Long residentId);

    List<InstaHelpBooking> findByCommunityIdAndStatusOrderByBookedAtDesc(
            Long communityId, InstaHelpBooking.BookingStatus status);

    Optional<InstaHelpBooking> findByBookingRef(String bookingRef);
}
