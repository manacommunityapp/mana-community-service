package com.manacommunity.api.amenity.service;

import com.manacommunity.api.amenity.entity.BookingWaitlist;
import com.manacommunity.api.amenity.entity.FacilitySlot;
import com.manacommunity.api.amenity.repository.BookingWaitlistRepository;
import com.manacommunity.api.amenity.repository.FacilitySlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistPromotionService {

    private final BookingWaitlistRepository waitlistRepository;
    private final FacilitySlotRepository slotRepository;
    private final SlotBookingService bookingService;

    private static final Duration OFFER_WINDOW = Duration.ofMinutes(15);

    @Transactional
    public WaitlistResult joinWaitlist(Long slotId, Long residentId, String flatNo) {
        FacilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

        if ("AVAILABLE".equals(slot.getStatus())) {
            return new WaitlistResult(false, "Slot is available — book it directly instead of waitlisting", null);
        }

        Optional<BookingWaitlist> existing = waitlistRepository.findBySlotIdAndResidentId(slotId, residentId);
        if (existing.isPresent()) {
            return new WaitlistResult(false, "Already on the waitlist at position " + existing.get().getPosition(), existing.get().getPosition());
        }

        int nextPosition = waitlistRepository.findMaxPosition(slotId).orElse(0) + 1;

        BookingWaitlist entry = BookingWaitlist.builder()
                .slotId(slotId)
                .facilityId(slot.getFacilityId())
                .communityId(slot.getCommunityId())
                .residentId(residentId)
                .flatNo(flatNo)
                .position(nextPosition)
                .status("WAITING")
                .build();
        waitlistRepository.save(entry);

        log.info("Resident {} joined waitlist for slot {} at position {}", residentId, slotId, nextPosition);
        return new WaitlistResult(true, "Added to waitlist at position " + nextPosition, nextPosition);
    }

    @Transactional
    public PromotionResult promoteNextInLine(Long slotId) {
        FacilitySlot slot = slotRepository.findByIdWithPessimisticLock(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

        if (!"AVAILABLE".equals(slot.getStatus())) {
            return new PromotionResult(false, "Slot is not available for promotion", null, null);
        }

        List<BookingWaitlist> waiters = waitlistRepository
                .findBySlotIdAndStatusOrderByPositionAsc(slotId, "WAITING");

        if (waiters.isEmpty()) {
            return new PromotionResult(false, "No one on the waitlist", null, null);
        }

        BookingWaitlist next = waiters.get(0);
        Instant now = Instant.now();
        next.setStatus("OFFERED");
        next.setOfferedAt(now);
        next.setOfferExpiresAt(now.plus(OFFER_WINDOW));
        waitlistRepository.save(next);

        slot.setStatus("HELD");
        slotRepository.save(slot);

        log.info("Slot {} offered to resident {} (position {}), expires at {}",
                slotId, next.getResidentId(), next.getPosition(), next.getOfferExpiresAt());

        return new PromotionResult(true, "Offered to waitlist position " + next.getPosition(),
                next.getResidentId(), next.getOfferExpiresAt());
    }

    @Transactional
    public SlotBookingService.BookingResult acceptOffer(Long slotId, Long residentId) {
        BookingWaitlist entry = waitlistRepository.findBySlotIdAndResidentId(slotId, residentId)
                .orElseThrow(() -> new IllegalArgumentException("No waitlist entry found"));

        if (!"OFFERED".equals(entry.getStatus())) {
            return new SlotBookingService.BookingResult(false, "No active offer for this resident", null, slotId);
        }

        if (entry.getOfferExpiresAt() != null && Instant.now().isAfter(entry.getOfferExpiresAt())) {
            entry.setStatus("EXPIRED");
            waitlistRepository.save(entry);
            return new SlotBookingService.BookingResult(false, "Offer has expired", null, slotId);
        }

        FacilitySlot slot = slotRepository.findByIdWithPessimisticLock(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        slot.setStatus("AVAILABLE");
        slotRepository.save(slot);

        entry.setStatus("ACCEPTED");
        waitlistRepository.save(entry);

        return bookingService.bookSlot(slotId, residentId, entry.getFlatNo());
    }

    @Transactional
    public int processExpiredOffers() {
        List<BookingWaitlist> expired = waitlistRepository.findExpiredOffers(Instant.now());
        int promoted = 0;

        for (BookingWaitlist entry : expired) {
            entry.setStatus("EXPIRED");
            waitlistRepository.save(entry);
            log.info("Offer expired for resident {} on slot {}", entry.getResidentId(), entry.getSlotId());

            FacilitySlot slot = slotRepository.findById(entry.getSlotId()).orElse(null);
            if (slot != null && "HELD".equals(slot.getStatus())) {
                slot.setStatus("AVAILABLE");
                slotRepository.save(slot);

                PromotionResult result = promoteNextInLine(entry.getSlotId());
                if (result.success()) {
                    promoted++;
                }
            }
        }

        return promoted;
    }

    @Transactional
    public void cancelFromWaitlist(Long slotId, Long residentId) {
        BookingWaitlist entry = waitlistRepository.findBySlotIdAndResidentId(slotId, residentId)
                .orElseThrow(() -> new IllegalArgumentException("Not on waitlist"));

        if ("OFFERED".equals(entry.getStatus())) {
            FacilitySlot slot = slotRepository.findById(slotId).orElse(null);
            if (slot != null && "HELD".equals(slot.getStatus())) {
                slot.setStatus("AVAILABLE");
                slotRepository.save(slot);
                promoteNextInLine(slotId);
            }
        }

        entry.setStatus("CANCELLED");
        waitlistRepository.save(entry);
    }

    public long getWaitlistSize(Long slotId) {
        return waitlistRepository.countBySlotIdAndStatus(slotId, "WAITING");
    }

    public List<BookingWaitlist> getMyWaitlistEntries(Long residentId) {
        return waitlistRepository.findByResidentIdAndStatusIn(residentId, List.of("WAITING", "OFFERED"));
    }

    public record WaitlistResult(boolean success, String message, Integer position) {}
    public record PromotionResult(boolean success, String message, Long offeredToResidentId, Instant offerExpiresAt) {}
}
