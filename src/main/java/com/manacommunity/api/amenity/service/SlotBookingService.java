package com.manacommunity.api.amenity.service;

import com.manacommunity.api.amenity.entity.*;
import com.manacommunity.api.amenity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotBookingService {

    private final FacilityRepository facilityRepository;
    private final FacilitySlotRepository slotRepository;
    private final FlatBookingQuotaRepository quotaRepository;
    private final BookingWaitlistRepository waitlistRepository;

    @Transactional
    public BookingResult bookSlot(Long slotId, Long residentId, String flatNo) {
        FacilitySlot slot = slotRepository.findByIdWithPessimisticLock(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (!"AVAILABLE".equals(slot.getStatus())) {
            return new BookingResult(false, "Slot is no longer available (status: " + slot.getStatus() + ")", null, slot.getId());
        }

        Facility facility = facilityRepository.findById(slot.getFacilityId())
                .orElseThrow(() -> new IllegalStateException("Facility not found"));

        QuotaCheckResult quotaCheck = checkAndReserveQuota(facility, slot, flatNo);
        if (!quotaCheck.allowed()) {
            return new BookingResult(false, quotaCheck.reason(), null, slot.getId());
        }

        String bookingRef = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        slot.setStatus("BOOKED");
        slot.setBookedByResidentId(residentId);
        slot.setBookedByFlatNo(flatNo);
        slot.setBookingReference(bookingRef);
        slotRepository.save(slot);

        log.info("Slot {} booked by resident {} (flat {}) — ref {}", slotId, residentId, flatNo, bookingRef);
        return new BookingResult(true, "Booked successfully", bookingRef, slot.getId());
    }

    @Transactional
    public CancelResult cancelBooking(Long slotId, Long residentId) {
        FacilitySlot slot = slotRepository.findByIdWithPessimisticLock(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (!"BOOKED".equals(slot.getStatus())) {
            return new CancelResult(false, "Slot is not currently booked", null);
        }
        if (!residentId.equals(slot.getBookedByResidentId())) {
            return new CancelResult(false, "Only the booking holder can cancel", null);
        }

        Facility facility = facilityRepository.findById(slot.getFacilityId())
                .orElseThrow(() -> new IllegalStateException("Facility not found"));

        releaseQuota(facility, slot);

        String oldRef = slot.getBookingReference();
        slot.setBookedByResidentId(null);
        slot.setBookedByFlatNo(null);
        slot.setBookingReference(null);

        // Atomically hold cancelled slot for position #1 on waitlist so no third party snatches it
        List<BookingWaitlist> waiters = waitlistRepository.findBySlotIdAndStatusOrderByPositionAsc(slotId, "WAITING");
        if (!waiters.isEmpty()) {
            BookingWaitlist next = waiters.get(0);
            Instant now = Instant.now();
            next.setStatus("OFFERED");
            next.setOfferedAt(now);
            next.setOfferExpiresAt(now.plus(Duration.ofMinutes(15)));
            waitlistRepository.save(next);

            slot.setStatus("HELD");
            slotRepository.save(slot);
            log.info("Booking {} cancelled for slot {}; immediately offered HOLD to waitlist resident {} (pos {})",
                    oldRef, slotId, next.getResidentId(), next.getPosition());
            return new CancelResult(true, "Cancelled and offered slot to waitlist position " + next.getPosition(),
                    oldRef, next.getResidentId(), next.getPosition());
        }

        slot.setStatus("AVAILABLE");
        slotRepository.save(slot);

        log.info("Booking {} cancelled for slot {}", oldRef, slotId);
        return new CancelResult(true, "Cancelled successfully", oldRef);
    }

    public List<FacilitySlot> getAvailableSlots(Long facilityId, LocalDate date) {
        return slotRepository.findAvailableSlots(facilityId, date);
    }

    public List<FacilitySlot> getSlotsForDate(Long facilityId, LocalDate date) {
        return slotRepository.findByFacilityIdAndSlotDate(facilityId, date);
    }

    @Transactional
    public List<FacilitySlot> generateSlotsForDate(Long facilityId, LocalDate date) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        List<FacilitySlot> existing = slotRepository.findByFacilityIdAndSlotDate(facilityId, date);
        if (!existing.isEmpty()) {
            return existing;
        }

        List<FacilitySlot> slots = new ArrayList<>();
        LocalTime time = LocalTime.of(facility.getOpenHour(), 0);
        LocalTime close = LocalTime.of(facility.getCloseHour(), 0);

        while (time.plusMinutes(facility.getSlotDurationMinutes()).compareTo(close) <= 0) {
            LocalTime end = time.plusMinutes(facility.getSlotDurationMinutes());
            boolean isPeak = time.getHour() >= facility.getPeakStartHour()
                    && time.getHour() < facility.getPeakEndHour();

            FacilitySlot slot = FacilitySlot.builder()
                    .facilityId(facilityId)
                    .communityId(facility.getCommunityId())
                    .slotDate(date)
                    .startTime(time)
                    .endTime(end)
                    .status("AVAILABLE")
                    .isPeakHour(isPeak)
                    .build();
            slots.add(slot);
            time = end;
        }

        return slotRepository.saveAll(slots);
    }

    private QuotaCheckResult checkAndReserveQuota(Facility facility, FacilitySlot slot, String flatNo) {
        LocalDate weekStart = slot.getSlotDate().with(DayOfWeek.MONDAY);

        FlatBookingQuota quota = quotaRepository.findForUpdate(
                facility.getCommunityId(), flatNo, facility.getId(), weekStart)
                .orElse(FlatBookingQuota.builder()
                        .communityId(facility.getCommunityId())
                        .flatNo(flatNo)
                        .facilityId(facility.getId())
                        .weekStart(weekStart)
                        .bookingsUsed(0)
                        .activePeakBookings(0)
                        .build());

        if (quota.getBookingsUsed() >= facility.getMaxBookingsPerFlatPerWeek()) {
            return new QuotaCheckResult(false,
                    "Weekly booking limit reached (" + facility.getMaxBookingsPerFlatPerWeek() + " per week for " + facility.getName() + ")");
        }

        if (slot.getIsPeakHour() && quota.getActivePeakBookings() >= facility.getMaxConcurrentPeakBookings()) {
            return new QuotaCheckResult(false,
                    "Cannot hold more than " + facility.getMaxConcurrentPeakBookings() + " active peak-hour booking(s) for " + facility.getName());
        }

        quota.setBookingsUsed(quota.getBookingsUsed() + 1);
        if (slot.getIsPeakHour()) {
            quota.setActivePeakBookings(quota.getActivePeakBookings() + 1);
        }
        quotaRepository.save(quota);

        return new QuotaCheckResult(true, null);
    }

    private void releaseQuota(Facility facility, FacilitySlot slot) {
        LocalDate weekStart = slot.getSlotDate().with(DayOfWeek.MONDAY);
        quotaRepository.findByCommunityIdAndFlatNoAndFacilityIdAndWeekStart(
                facility.getCommunityId(), slot.getBookedByFlatNo(), facility.getId(), weekStart)
                .ifPresent(quota -> {
                    quota.setBookingsUsed(Math.max(0, quota.getBookingsUsed() - 1));
                    if (slot.getIsPeakHour()) {
                        quota.setActivePeakBookings(Math.max(0, quota.getActivePeakBookings() - 1));
                    }
                    quotaRepository.save(quota);
                });
    }

    public record BookingResult(boolean success, String message, String bookingReference, Long slotId) {}
    public record CancelResult(
            boolean success,
            String message,
            String cancelledReference,
            Long promotedWaitlistResidentId,
            Integer promotedWaitlistPosition
    ) {
        public CancelResult(boolean success, String message, String cancelledReference) {
            this(success, message, cancelledReference, null, null);
        }
    }
    private record QuotaCheckResult(boolean allowed, String reason) {}
}
