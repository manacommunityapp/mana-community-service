package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsCourtSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SportsCourtSlotRepository extends JpaRepository<SportsCourtSlot, Long> {

    List<SportsCourtSlot> findByCourtIdAndSlotDateOrderByStartTimeAsc(Long courtId, LocalDate slotDate);

    Optional<SportsCourtSlot> findByCourtIdAndSlotDateAndStartTime(Long courtId, LocalDate slotDate, String startTime);

    List<SportsCourtSlot> findByStatusAndHeldUntilBefore(String status, LocalDateTime expiryTime);
}
