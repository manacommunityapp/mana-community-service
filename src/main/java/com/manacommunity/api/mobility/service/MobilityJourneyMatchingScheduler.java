package com.manacommunity.api.mobility.service;

import com.manacommunity.api.mobility.model.MobilityAirportRideRequest;
import com.manacommunity.api.mobility.model.MobilityCarpoolBooking;
import com.manacommunity.api.mobility.model.MobilityCarpoolOffer;
import com.manacommunity.api.mobility.model.MobilityJourneyMatch;
import com.manacommunity.api.mobility.model.MobilityJourneySlot;
import com.manacommunity.api.mobility.repository.MobilityAirportRideRequestRepository;
import com.manacommunity.api.mobility.repository.MobilityCarpoolBookingRepository;
import com.manacommunity.api.mobility.repository.MobilityCarpoolOfferRepository;
import com.manacommunity.api.mobility.repository.MobilityJourneyMatchRepository;
import com.manacommunity.api.mobility.repository.MobilityJourneySlotRepository;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.trip.engine.CommuteMatchingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MobilityJourneyMatchingScheduler {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final MobilityAirportRideRequestRepository requestRepo;
    private final MobilityCarpoolOfferRepository offerRepo;
    private final MobilityJourneyMatchRepository matchRepo;
    private final MobilityJourneySlotRepository slotRepo;
    private final MobilityCarpoolBookingRepository bookingRepo;
    private final CommuteMatchingEngine matchingEngine;
    private final ExpoPushService expoPushService;

    @Scheduled(fixedDelay = 900_000L)
    @Transactional
    public void notifyOpenJourneyMatches() {
        Instant now = Instant.now();
        generateRecurringSlotOffers(now);
        expireStaleJourneys(now);
        for (MobilityAirportRideRequest request : requestRepo.findByStatusAndTravelAtBetweenOrderByTravelAtAsc(
                "OPEN", now, now.plus(Duration.ofHours(48)))) {
            if (request.getJourneyType() == null || request.getHubCode() == null) continue;
            if (request.getLastMatchNotifiedAt() != null && request.getLastMatchNotifiedAt().isAfter(now.minus(Duration.ofHours(6)))) continue;
            long window = Math.max(15, Math.min(request.getTimeWindowMinutes(), 180)) * 60_000L;
            Page<MobilityCarpoolOffer> matches = offerRepo.findJourneyOffersWithScope(
                    request.getCommunity().getId(), request.isAllowCrossCommunity(),
                    request.getJourneyType(), request.getHubCode(), request.getDirection(),
                    request.getArea(), request.getTravelAt().minusMillis(window), request.getTravelAt().plusMillis(window),
                    request.getSeats(), request.getLuggageCount(), PageRequest.of(0, 10));
            if (matches.isEmpty()) continue;

            double maxDetour = request.getMaxDetourKm() != null ? request.getMaxDetourKm() : 5.0;
            List<MobilityCarpoolOffer> eligible = matches.getContent().stream()
                    .filter(offer -> !offer.getDriverUser().getId().equals(request.getRequester().getId()))
                    .filter(offer -> withinDetour(request, offer, maxDetour))
                    .toList();
            if (eligible.isEmpty()) continue;

            boolean reserved = false;
            boolean consentOk = !request.isParentConsentRequired()
                    || "APPROVED".equalsIgnoreCase(request.getGuardianConsentStatus());

            for (MobilityCarpoolOffer offer : eligible) {
                boolean alreadyBooked = bookingRepo.existsByOfferIdAndUserIdAndStatus(
                        offer.getId(), request.getRequester().getId(), "CONFIRMED");
                if (request.isAutoReserve() && !reserved && consentOk && !alreadyBooked
                        && offer.getAvailableSeats() >= request.getSeats()) {
                    reserveSeatForRequest(request, offer);
                    reserved = true;
                    continue;
                }
                if (matchRepo.findByRequestIdAndOfferId(request.getId(), offer.getId()).isEmpty()) {
                    matchRepo.save(MobilityJourneyMatch.builder().request(request).offer(offer)
                            .requester(request.getRequester()).status("PROPOSED").build());
                }
            }
            String title = reserved ? "Recurring seat automatically reserved" : "A shared journey is forming";
            String body = reserved
                    ? "Reserved " + request.getSeats() + " seat(s) for your " + request.getJourneyType().toLowerCase() + " journey near " + request.getHubCode() + "."
                    : eligible.size() + " compatible " + request.getJourneyType().toLowerCase() + " ride(s) found near " + request.getHubCode() + ".";
            expoPushService.sendToUser(request.getRequester().getId(), title, body,
                    Map.of("type", reserved ? "MOBILITY_AUTO_RESERVED" : "MOBILITY_GROUP_MATCH", "requestId", request.getId()), "default");
            request.setLastMatchNotifiedAt(now);
            requestRepo.save(request);
        }
    }

    public int generateRecurringSlotOffers(Instant now) {
        LocalDate today = now.atZone(IST).toLocalDate();
        int created = 0;
        for (MobilityJourneySlot slot : slotRepo.findByStatusAndAutoGenerateTrue("ACTIVE")) {
            for (int offset = 0; offset <= 1; offset++) {
                LocalDate targetDate = today.plusDays(offset);
                if (!matchesDayOfWeek(slot.getDaysOfWeek(), targetDate.getDayOfWeek())) continue;
                Instant departureAt = targetDate.atTime(slot.getDepartureTime()).atZone(IST).toInstant();
                if (departureAt.isBefore(now.plus(Duration.ofMinutes(15)))) continue;
                if (offerRepo.existsBySourceSlotIdAndScheduledDate(slot.getId(), targetDate)) continue;

                offerRepo.save(MobilityCarpoolOffer.builder()
                        .community(slot.getCommunity())
                        .driverUser(slot.getCreatedBy())
                        .origin(slot.getOrigin())
                        .destination(slot.getDestination())
                        .journeyType(slot.getJourneyType())
                        .journeyDirection(slot.getDirection())
                        .hubCode(slot.getHubCode())
                        .luggageCapacity(slot.getLuggageCapacity())
                        .departureTime(departureAt)
                        .totalSeats(slot.getSeatCapacity())
                        .availableSeats(slot.getSeatCapacity())
                        .pricePerSeat(slot.getPricePerSeat() != null ? slot.getPricePerSeat() : BigDecimal.ZERO)
                        .originLat(slot.getOriginLat())
                        .originLng(slot.getOriginLng())
                        .destLat(slot.getDestLat())
                        .destLng(slot.getDestLng())
                        .maxDetourKm(slot.getMaxDetourKm() != null ? slot.getMaxDetourKm() : 5.0)
                        .allowCrossCommunity(slot.isAllowCrossCommunity())
                        .sourceSlotId(slot.getId())
                        .scheduledDate(targetDate)
                        .parentConsentRequired("SCHOOL".equalsIgnoreCase(slot.getJourneyType()))
                        .status("ACTIVE")
                        .build());
                slot.setLastGeneratedDate(targetDate);
                slotRepo.save(slot);
                created++;
            }
        }
        return created;
    }

    private void reserveSeatForRequest(MobilityAirportRideRequest request, MobilityCarpoolOffer offer) {
        offer.setAvailableSeats((short) (offer.getAvailableSeats() - request.getSeats()));
        if (offer.getAvailableSeats() == 0) offer.setStatus("FULL");
        offerRepo.save(offer);

        BigDecimal totalAmt = (offer.getPricePerSeat() != null ? offer.getPricePerSeat() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(request.getSeats())).setScale(2, RoundingMode.HALF_UP);
        bookingRepo.save(MobilityCarpoolBooking.builder()
                .offer(offer)
                .user(request.getRequester())
                .seats(request.getSeats())
                .status("CONFIRMED")
                .totalAmount(totalAmt)
                .paymentStatus("CASH")
                .guardianConsentStatus(request.getGuardianConsentStatus())
                .guardianName(request.getGuardianName())
                .guardianPhone(request.getGuardianPhone())
                .guardianConsentedAt(request.getGuardianConsentedAt())
                .build());
        matchRepo.save(MobilityJourneyMatch.builder()
                .request(request).offer(offer).requester(request.getRequester())
                .status("ACCEPTED").respondedAt(Instant.now()).build());
        request.setStatus("MATCHED");
    }

    private boolean withinDetour(MobilityAirportRideRequest req, MobilityCarpoolOffer offer, double maxDetourKm) {
        double detour = 0.0;
        boolean hasGeo = false;
        if (req.getPickupLat() != null && req.getPickupLng() != null && offer.getOriginLat() != null && offer.getOriginLng() != null) {
            detour += matchingEngine.calculateDistanceKm(req.getPickupLat(), req.getPickupLng(), offer.getOriginLat(), offer.getOriginLng());
            hasGeo = true;
        }
        if (req.getDestLat() != null && req.getDestLng() != null && offer.getDestLat() != null && offer.getDestLng() != null) {
            detour += matchingEngine.calculateDistanceKm(req.getDestLat(), req.getDestLng(), offer.getDestLat(), offer.getDestLng());
            hasGeo = true;
        }
        double limit = offer.getMaxDetourKm() != null ? Math.min(maxDetourKm, offer.getMaxDetourKm()) : maxDetourKm;
        return !hasGeo || detour <= limit;
    }

    private boolean matchesDayOfWeek(String daysSpec, DayOfWeek day) {
        if (daysSpec == null || daysSpec.isBlank()) return false;
        String norm = daysSpec.trim().toUpperCase(Locale.ROOT);
        if ("DAILY".equals(norm) || "ALL".equals(norm)) return true;
        if ("WEEKDAYS".equals(norm)) return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
        if ("WEEKENDS".equals(norm)) return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        String shortName = day.name().substring(0, 3);
        return norm.contains(shortName);
    }

    private void expireStaleJourneys(Instant now) {
        requestRepo.findByStatusAndTravelAtBefore("OPEN", now).forEach(request -> {
            request.setStatus("EXPIRED");
            requestRepo.save(request);
            expoPushService.sendToUser(request.getRequester().getId(), "Journey request expired",
                    "The requested travel time has passed without a confirmed shared journey.",
                    Map.of("type", "MOBILITY_REQUEST_EXPIRED", "requestId", request.getId()), "default");
        });
        offerRepo.findByStatusInAndDepartureTimeBefore(java.util.List.of("ACTIVE", "FULL"), now).forEach(offer -> {
            offer.setStatus("COMPLETED");
            offerRepo.save(offer);
        });
    }
}
