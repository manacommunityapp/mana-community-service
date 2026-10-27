package com.manacommunity.api.mobility.service;

import com.manacommunity.api.mobility.model.MobilityAirportRideRequest;
import com.manacommunity.api.mobility.repository.MobilityAirportRideRequestRepository;
import com.manacommunity.api.mobility.repository.MobilityCarpoolOfferRepository;
import com.manacommunity.api.mobility.repository.MobilityJourneyMatchRepository;
import com.manacommunity.api.mobility.model.MobilityJourneyMatch;
import com.manacommunity.api.service.ExpoPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MobilityJourneyMatchingScheduler {
    private final MobilityAirportRideRequestRepository requestRepo;
    private final MobilityCarpoolOfferRepository offerRepo;
    private final MobilityJourneyMatchRepository matchRepo;
    private final ExpoPushService expoPushService;

    @Scheduled(fixedDelay = 900_000L)
    public void notifyOpenJourneyMatches() {
        Instant now = Instant.now();
        for (MobilityAirportRideRequest request : requestRepo.findByStatusAndTravelAtBetweenOrderByTravelAtAsc(
                "OPEN", now, now.plus(Duration.ofHours(48)))) {
            if (request.getJourneyType() == null || request.getHubCode() == null) continue;
            if (request.getLastMatchNotifiedAt() != null && request.getLastMatchNotifiedAt().isAfter(now.minus(Duration.ofHours(6)))) continue;
            long window = Math.max(15, Math.min(request.getTimeWindowMinutes(), 180)) * 60_000L;
            Page<?> matches = offerRepo.findJourneyOffers(request.getCommunity().getId(), request.getJourneyType(), request.getHubCode(),
                    request.getArea(), request.getTravelAt().minusMillis(window), request.getTravelAt().plusMillis(window),
                    request.getSeats(), request.getLuggageCount(), PageRequest.of(0, 5));
            if (matches.isEmpty()) continue;
            matches.getContent().forEach(item -> {
                var offer = (com.manacommunity.api.mobility.model.MobilityCarpoolOffer) item;
                if (offer.getDriverUser().getId().equals(request.getRequester().getId())) return;
                if (matchRepo.findByRequestIdAndOfferId(request.getId(), offer.getId()).isEmpty()) {
                    matchRepo.save(MobilityJourneyMatch.builder().request(request).offer(offer)
                            .requester(request.getRequester()).status("PROPOSED").build());
                }
            });
            expoPushService.sendToUser(request.getRequester().getId(), "A shared journey is forming",
                    matches.getTotalElements() + " compatible " + request.getJourneyType().toLowerCase() + " ride(s) found near " + request.getHubCode() + ".",
                    Map.of("type", "MOBILITY_GROUP_MATCH", "requestId", request.getId()), "default");
            request.setLastMatchNotifiedAt(now);
            requestRepo.save(request);
        }
    }
}
