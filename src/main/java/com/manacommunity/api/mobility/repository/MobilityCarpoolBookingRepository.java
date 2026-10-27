package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityCarpoolBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;

public interface MobilityCarpoolBookingRepository extends JpaRepository<MobilityCarpoolBooking, Long> {
    Optional<MobilityCarpoolBooking> findByOfferIdAndUserId(Long offerId, Long userId);
    Optional<MobilityCarpoolBooking> findFirstByOfferIdAndStatusOrderByBookedAtAsc(Long offerId, String status);
    Page<MobilityCarpoolBooking> findByUserIdOrderByBookedAtDesc(Long userId, Pageable pageable);
    List<MobilityCarpoolBooking> findByOfferIdAndStatusOrderByBookedAtAsc(Long offerId, String status);
}
