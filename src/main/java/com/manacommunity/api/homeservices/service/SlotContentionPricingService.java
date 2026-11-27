package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.entity.HsBooking;
import com.manacommunity.api.homeservices.entity.HsWorkerAvailability;
import com.manacommunity.api.homeservices.repository.HsBookingRepository;
import com.manacommunity.api.homeservices.repository.HsWorkerAvailabilityRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Home Service Dynamic Pricing + Inter-Community Travel Buffer + Two-Phase Slot Locking Engine.
 * Prevents double-booking the last technician while rewarding same-society slot clustering.
 */
@Service
@RequiredArgsConstructor
public class SlotContentionPricingService {

    public static final int DEFAULT_HOLD_SECONDS = 300; // 5-min checkout hold

    private final HsWorkerAvailabilityRepository availabilityRepo;
    private final HsBookingRepository bookingRepo;

    @Builder
    public record DynamicPriceQuote(
            BigDecimal basePrice,
            BigDecimal weekendMultiplier,
            BigDecimal surgeMultiplier,
            BigDecimal clusterDiscount,
            BigDecimal quotedPrice,
            int travelBufferMinutes,
            String pricingReason
    ) {}

    public DynamicPriceQuote calculateDynamicPrice(
            BigDecimal basePrice,
            boolean isWeekend,
            int zoneUtilizationPct,
            boolean sameCommunityCluster
    ) {
        BigDecimal base = basePrice != null ? basePrice : BigDecimal.ZERO;
        BigDecimal weekendMult = isWeekend ? new BigDecimal("1.15") : BigDecimal.ONE;
        BigDecimal surgeMult;
        if (zoneUtilizationPct >= 85) {
            surgeMult = new BigDecimal("1.30");
        } else if (zoneUtilizationPct >= 65) {
            surgeMult = new BigDecimal("1.15");
        } else {
            surgeMult = BigDecimal.ONE;
        }

        BigDecimal rawTotal = base.multiply(weekendMult).multiply(surgeMult).setScale(2, RoundingMode.HALF_UP);
        BigDecimal clusterDiscount = sameCommunityCluster
                ? rawTotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal finalQuote = rawTotal.subtract(clusterDiscount).max(BigDecimal.ZERO);

        return DynamicPriceQuote.builder()
                .basePrice(base)
                .weekendMultiplier(weekendMult)
                .surgeMultiplier(surgeMult)
                .clusterDiscount(clusterDiscount)
                .quotedPrice(finalQuote)
                .travelBufferMinutes(sameCommunityCluster ? 5 : 25)
                .pricingReason(sameCommunityCluster ? "Same-society cluster discount applied" : "Inter-community transit buffer included")
                .build();
    }

    /**
     * Checks whether a candidate slot is clear of inter-community transit buffers from existing booked slots.
     */
    public boolean isSlotClearWithTravelBuffer(
            Long workerId,
            Long targetCommunityId,
            LocalDate date,
            LocalTime candidateStart,
            LocalTime candidateEnd
    ) {
        List<HsWorkerAvailability> daySlots = availabilityRepo.findByWorkerIdAndAvailableDateGreaterThanEqual(workerId, date);
        for (HsWorkerAvailability s : daySlots) {
            if (!date.equals(s.getAvailableDate()) || !Boolean.TRUE.equals(s.getIsBooked())) {
                continue;
            }
            int bufferMins = targetCommunityId.equals(s.getCommunityId())
                    ? 5
                    : (s.getTravelBufferMinutes() != null ? s.getTravelBufferMinutes() : 25);
            LocalTime blockedFrom = s.getSlotStart().minusMinutes(bufferMins);
            LocalTime blockedUntil = s.getSlotEnd().plusMinutes(bufferMins);

            boolean overlaps = candidateStart.isBefore(blockedUntil) && candidateEnd.isAfter(blockedFrom);
            if (overlaps) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public HsWorkerAvailability acquireCheckoutHold(Long slotId, String holdToken, Instant now) {
        Instant ref = now != null ? now : Instant.now();
        HsWorkerAvailability slot = availabilityRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (Boolean.TRUE.equals(slot.getIsBooked())) {
            throw new IllegalStateException("409 SLOT_CONFLICT: Slot already booked by another resident");
        }
        if (slot.getHoldToken() != null && !slot.getHoldToken().equals(holdToken)
                && slot.getHoldExpiresAt() != null && slot.getHoldExpiresAt().isAfter(ref)) {
            throw new IllegalStateException("409 SLOT_HELD: Another resident is currently checking out for this slot");
        }

        slot.setHoldToken(holdToken);
        slot.setHoldExpiresAt(ref.plus(Duration.ofSeconds(DEFAULT_HOLD_SECONDS)));
        return availabilityRepo.save(slot);
    }

    @Transactional
    public HsBooking confirmHeldSlotBooking(
            Long slotId,
            String holdToken,
            Long targetCommunityId,
            Long userId,
            String flatNumber,
            String category,
            BigDecimal quotedPrice,
            Instant now
    ) {
        Instant ref = now != null ? now : Instant.now();
        HsWorkerAvailability slot = availabilityRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (Boolean.TRUE.equals(slot.getIsBooked())) {
            throw new IllegalStateException("409 SLOT_CONFLICT: Technician just booked by another resident");
        }
        if (slot.getHoldToken() != null && !slot.getHoldToken().equals(holdToken)
                && slot.getHoldExpiresAt() != null && slot.getHoldExpiresAt().isAfter(ref)) {
            throw new IllegalStateException("409 HOLD_MISMATCH: Slot held by a different checkout session");
        }
        if (!isSlotClearWithTravelBuffer(slot.getWorkerId(), targetCommunityId,
                slot.getAvailableDate(), slot.getSlotStart(), slot.getSlotEnd())) {
            throw new IllegalStateException("409 TRAVEL_BUFFER_CONFLICT: Technician cannot arrive across communities in time");
        }

        slot.setIsBooked(true);
        slot.setHoldToken(null);
        slot.setHoldExpiresAt(null);
        availabilityRepo.save(slot);

        return bookingRepo.save(HsBooking.builder()
                .communityId(targetCommunityId)
                .userId(userId)
                .workerId(slot.getWorkerId())
                .category(category != null ? category : "APPLIANCE")
                .bookingType("SCHEDULED")
                .scheduledAt(slot.getAvailableDate().atTime(slot.getSlotStart()).toInstant(java.time.ZoneOffset.UTC))
                .flatNumber(flatNumber)
                .quotedPrice(quotedPrice)
                .status("CONFIRMED")
                .build());
    }
}
