package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityCarpoolBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MobilityCarpoolBookingRepository extends JpaRepository<MobilityCarpoolBooking, Long> {
    Optional<MobilityCarpoolBooking> findByOfferIdAndUserId(Long offerId, Long userId);
    Optional<MobilityCarpoolBooking> findFirstByOfferIdAndStatusOrderByBookedAtAsc(Long offerId, String status);
}
