package com.manacommunity.api.parking.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.entity.ParkingSession;
import com.manacommunity.api.parking.entity.ParkingSlot;
import com.manacommunity.api.parking.entity.ParkingSlotBooking;
import com.manacommunity.api.parking.repository.ParkingSessionRepository;
import com.manacommunity.api.parking.repository.ParkingSlotBookingRepository;
import com.manacommunity.api.parking.repository.ParkingSlotRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingSessionService {

    private final ParkingSessionRepository sessionRepo;
    private final ParkingSlotBookingRepository bookingRepo;
    private final ParkingSlotRepository slotRepo;
    private final EntityManager em;

    public ParkingSession checkIn(Long communityId, Long slotId, String vehicleNumber,
                                   ParkingSession.SessionType type, Long authorizedBy) {
        ParkingSlot slot = slotId != null
                ? slotRepo.findById(slotId).orElse(null)
                : null;
        Community community = em.getReference(Community.class, communityId);
        ParkingSession session = ParkingSession.builder()
                .community(community)
                .slot(slot)
                .vehicleNumber(vehicleNumber.toUpperCase().replaceAll("\\s+", ""))
                .sessionType(type)
                .authorizedBy(authorizedBy)
                .build();
        return sessionRepo.save(session);
    }

    public ParkingSession checkOut(Long sessionId, Long communityId) {
        ParkingSession session = sessionRepo.findById(sessionId)
                .filter(s -> s.getCommunity().getId().equals(communityId))
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (session.getExitTime() != null) throw new IllegalStateException("Session already checked out");
        session.setExitTime(Instant.now());
        return sessionRepo.save(session);
    }

    public List<ParkingSession> getActiveSessions(Long communityId) {
        return sessionRepo.findByCommunityIdAndExitTimeIsNullOrderByEntryTimeDesc(communityId);
    }

    public long countActiveSessions(Long communityId) {
        return sessionRepo.countByCommunityIdAndExitTimeIsNull(communityId);
    }

    public List<ParkingSession> detectOverstays(Long communityId, int thresholdHours) {
        Instant threshold = Instant.now().minus(thresholdHours, ChronoUnit.HOURS);
        List<ParkingSession> candidates = sessionRepo.findPotentialOverstays(communityId, threshold);
        for (ParkingSession s : candidates) {
            if (!Boolean.TRUE.equals(s.getOverstayFlag())) {
                s.setOverstayFlag(true);
            }
        }
        return sessionRepo.saveAll(candidates);
    }

    public ParkingSlotBooking bookVisitorParking(Long communityId, Long residentId,
                                                  Long slotId, String guestName, String guestVehicle,
                                                  LocalDate date, java.time.LocalTime start, java.time.LocalTime end) {
        ParkingSlot slot = slotRepo.findById(slotId)
                .filter(s -> s.getCommunity().getId().equals(communityId))
                .orElseThrow(() -> new IllegalArgumentException("Slot not found in community"));
        Community community = em.getReference(Community.class, communityId);
        ParkingSlotBooking booking = ParkingSlotBooking.builder()
                .community(community)
                .slot(slot)
                .bookedBy(residentId)
                .guestName(guestName)
                .guestVehicle(guestVehicle)
                .bookingDate(date)
                .startTime(start)
                .endTime(end)
                .build();
        return bookingRepo.save(booking);
    }

    public void cancelBooking(Long bookingId, Long residentId) {
        ParkingSlotBooking booking = bookingRepo.findById(bookingId)
                .filter(b -> b.getBookedBy().equals(residentId))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        booking.setStatus(ParkingSlotBooking.BookingStatus.CANCELLED);
    }

    public List<ParkingSlotBooking> getMyBookings(Long residentId) {
        return bookingRepo.findByBookedByOrderByBookingDateDescStartTimeDesc(residentId);
    }
}
