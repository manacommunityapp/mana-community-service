package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.BookingApproval;
import com.manacommunity.api.booking.entity.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingApprovalRepository extends JpaRepository<BookingApproval, Long> {

    List<BookingApproval> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    Optional<BookingApproval> findByBookingIdAndStatus(Long bookingId, ApprovalStatus status);
}
