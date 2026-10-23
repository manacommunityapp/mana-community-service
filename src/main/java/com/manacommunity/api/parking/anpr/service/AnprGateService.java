package com.manacommunity.api.parking.anpr.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.anpr.dto.*;
import com.manacommunity.api.parking.anpr.entity.AnprGateEvent;
import com.manacommunity.api.parking.anpr.entity.AnprGateEvent.BarrierAction;
import com.manacommunity.api.parking.anpr.repository.AnprGateEventRepository;
import com.manacommunity.api.parking.entity.ParkingVisitorPass;
import com.manacommunity.api.parking.entity.ResidentVehicle;
import com.manacommunity.api.parking.repository.ParkingVisitorPassRepository;
import com.manacommunity.api.parking.repository.ResidentVehicleRepository;
import com.manacommunity.api.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Core ANPR business logic for the Java mana-community-service.
 *
 * Decision flow for each incoming plate event:
 *   1. Normalise the plate string (strip spaces/hyphens, uppercase)
 *   2. Look up in ResidentVehicle by plate number
 *      → Match found: BarrierAction = OPEN, log resident name
 *   3. Else look up in ParkingVisitorPass (ACTIVE, validUntil > now)
 *      → Match found: BarrierAction = OPEN, log visitor
 *   4. Else: BarrierAction = HOLD, push WebSocket alert to security dashboard
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnprGateService {

    private final AnprGateEventRepository eventRepository;
    private final ResidentVehicleRepository vehicleRepository;
    private final ParkingVisitorPassRepository visitorPassRepository;
    private final CommunityRepository communityRepository;
    private final AnprPlateNormalizer normalizer;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Webhook handler ───────────────────────────────────────────────────────

    @Transactional
    public AnprWebhookResponse processWebhook(Long communityId, AnprWebhookPayload payload) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

        String normalisedPlate = normalizer.normalize(payload.plateNumber());

        // Step 1: Match against registered resident vehicles
        Optional<ResidentVehicle> vehicleMatch = vehicleRepository
                .findByCommunityIdAndNumberPlate(communityId, normalisedPlate);

        if (vehicleMatch.isPresent()) {
            ResidentVehicle v = vehicleMatch.get();
            AnprGateEvent event = saveEvent(community, payload, normalisedPlate,
                    BarrierAction.OPEN, v.getId(), null,
                    v.getOwner().getFullName());

            log.info("ANPR OPEN — resident vehicle: plate={} resident={} gate={}",
                    normalisedPlate, v.getOwner().getFullName(), payload.gateId());

            return new AnprWebhookResponse(
                    event.getId(), normalisedPlate, BarrierAction.OPEN,
                    v.getOwner().getFullName(),
                    "Resident vehicle recognised — barrier opening");
        }

        // Step 2: Match against active visitor passes
        Optional<ParkingVisitorPass> passMatch = findActiveVisitorPass(communityId, normalisedPlate);

        if (passMatch.isPresent()) {
            ParkingVisitorPass pass = passMatch.get();
            AnprGateEvent event = saveEvent(community, payload, normalisedPlate,
                    BarrierAction.OPEN, null, pass.getId(),
                    pass.getVisitorName());

            log.info("ANPR OPEN — visitor pass: plate={} visitor={} gate={}",
                    normalisedPlate, pass.getVisitorName(), payload.gateId());

            return new AnprWebhookResponse(
                    event.getId(), normalisedPlate, BarrierAction.OPEN,
                    pass.getVisitorName(),
                    "Visitor pass valid — barrier opening");
        }

        // Step 3: Unknown vehicle — HOLD and alert security
        AnprGateEvent event = saveEvent(community, payload, normalisedPlate,
                BarrierAction.HOLD, null, null, null);

        pushSecurityAlert(communityId, event);

        log.warn("ANPR HOLD — unknown vehicle: plate={} gate={} confidence={}",
                normalisedPlate, payload.gateId(), payload.confidence());

        return new AnprWebhookResponse(
                event.getId(), normalisedPlate, BarrierAction.HOLD,
                null,
                "Unknown vehicle — security notified, barrier on hold");
    }

    // ── Query APIs ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AnprGateEventResponse> getEvents(Long communityId, String gateId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AnprGateEvent> events = gateId != null
                ? eventRepository.findByCommunityIdAndGateIdOrderByCreatedAtDesc(communityId, gateId, pageable)
                : eventRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable);
        return events.map(AnprGateEventResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AnprGateEventResponse> getPlateHistory(Long communityId, String plateNumber) {
        String normalised = normalizer.normalize(plateNumber);
        return eventRepository
                .findByCommunityIdAndPlateNumberOrderByCreatedAtDesc(communityId, normalised)
                .stream()
                .map(AnprGateEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnprGateEventResponse> getPendingAlerts(Long communityId) {
        LocalDateTime since = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        return eventRepository.findUnknownVehicleAlerts(communityId, since)
                .stream()
                .map(AnprGateEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnprGateSummaryResponse getDailySummary(Long communityId) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        long open = eventRepository.countByCommunityIdAndBarrierActionAndCreatedAtAfter(
                communityId, BarrierAction.OPEN, startOfDay);
        long hold = eventRepository.countByCommunityIdAndBarrierActionAndCreatedAtAfter(
                communityId, BarrierAction.HOLD, startOfDay);
        long deny = eventRepository.countByCommunityIdAndBarrierActionAndCreatedAtAfter(
                communityId, BarrierAction.DENY, startOfDay);
        return new AnprGateSummaryResponse(open + hold + deny, open, hold, deny, hold);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private AnprGateEvent saveEvent(
            Community community,
            AnprWebhookPayload payload,
            String normalisedPlate,
            BarrierAction barrierAction,
            Long vehicleId,
            Long visitorPassId,
            String residentName
    ) {
        AnprGateEvent event = AnprGateEvent.builder()
                .community(community)
                .gateId(payload.gateId())
                .direction(payload.direction() != null ? payload.direction() : "UNKNOWN")
                .plateNumber(normalisedPlate)
                .rawOcrText(payload.plateNumber())
                .confidence(payload.confidence())
                .anprStatus(payload.status())
                .barrierAction(barrierAction)
                .matchedVehicleId(vehicleId)
                .matchedVisitorPassId(visitorPassId)
                .matchedResidentName(residentName)
                .externalEventId(payload.eventId())
                .build();
        return eventRepository.save(event);
    }

    private Optional<ParkingVisitorPass> findActiveVisitorPass(Long communityId, String plateNumber) {
        return visitorPassRepository.findByCommunityIdAndStatus(communityId, "ACTIVE")
                .stream()
                .filter(p -> normalizer.matches(p.getVehicleNumber(), plateNumber))
                .filter(p -> p.getValidUntil() != null && p.getValidUntil().isAfter(LocalDateTime.now()))
                .findFirst();
    }

    private void pushSecurityAlert(Long communityId, AnprGateEvent event) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/community/" + communityId + "/anpr/alerts",
                    new AnprSecurityAlert(
                            event.getId(),
                            event.getGateId(),
                            event.getPlateNumber(),
                            event.getConfidence(),
                            event.getCreatedAt()
                    )
            );
        } catch (Exception ex) {
            log.warn("Could not push ANPR WebSocket alert: {}", ex.getMessage());
        }
    }

    /** Lightweight payload pushed to security dashboard via STOMP WebSocket. */
    public record AnprSecurityAlert(
            Long eventId,
            String gateId,
            String plateNumber,
            Double confidence,
            LocalDateTime detectedAt
    ) {}
}
