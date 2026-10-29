package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.TrainerSlotBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainerSlotBookingRepository extends JpaRepository<TrainerSlotBooking, Long> {

    List<TrainerSlotBooking> findByUserIdAndCommunityIdOrderByBookedAtDesc(Long userId, Long communityId);

    boolean existsBySlot_IdAndUserIdAndStatus(Long slotId, Long userId, String status);

    long countBySlot_IdAndStatus(Long slotId, String status);
}
