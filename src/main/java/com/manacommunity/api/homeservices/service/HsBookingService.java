package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.entity.*;
import com.manacommunity.api.homeservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HsBookingService {

    private final HsBookingRepository bookingRepo;
    private final HsWorkerAvailabilityRepository availRepo;

    @Transactional
    public HsBooking create(HsBooking booking) {
        return bookingRepo.save(booking);
    }

    public List<HsBooking> myBookings(Long userId) {
        return bookingRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<HsBooking> communityPending(Long communityId) {
        return bookingRepo.findByCommunityIdAndStatus(communityId, "PENDING");
    }

    @Transactional
    public HsBooking assignWorker(Long bookingId, Long workerId) {
        HsBooking b = bookingRepo.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        b.setWorkerId(workerId);
        b.setStatus("ASSIGNED");
        b.setUpdatedAt(Instant.now());
        return bookingRepo.save(b);
    }

    @Transactional
    public HsBooking updateStatus(Long bookingId, String status) {
        HsBooking b = bookingRepo.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        b.setStatus(status);
        b.setUpdatedAt(Instant.now());
        return bookingRepo.save(b);
    }

    @Transactional
    public HsBooking cancel(Long bookingId, Long requestingUserId, Long communityId) {
        HsBooking b = bookingRepo.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!b.getCommunityId().equals(communityId)) throw new SecurityException("Community mismatch");
        if (!b.getUserId().equals(requestingUserId)) throw new SecurityException("Not your booking");
        if (List.of("WORK_DONE","COMPLETED").contains(b.getStatus()))
            throw new IllegalStateException("Cannot cancel completed booking");
        b.setStatus("CANCELLED");
        b.setUpdatedAt(Instant.now());
        return bookingRepo.save(b);
    }
}
