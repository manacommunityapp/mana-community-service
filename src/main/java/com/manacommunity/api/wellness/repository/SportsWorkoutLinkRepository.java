package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.SportsWorkoutLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SportsWorkoutLinkRepository extends JpaRepository<SportsWorkoutLink, Long> {
    List<SportsWorkoutLink> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SportsWorkoutLink> findByUserIdAndBookingId(Long userId, Long bookingId);
}
