package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.BookingWaitlist;
import com.manacommunity.api.booking.entity.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingWaitlistRepository extends JpaRepository<BookingWaitlist, Long> {

    List<BookingWaitlist> findByResourceIdAndRequestedDateAndStatusOrderByPositionAsc(
            Long resourceId, LocalDate date, WaitlistStatus status);

    List<BookingWaitlist> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, WaitlistStatus status);

    long countByResourceIdAndRequestedDateAndStatus(Long resourceId, LocalDate date, WaitlistStatus status);
}
