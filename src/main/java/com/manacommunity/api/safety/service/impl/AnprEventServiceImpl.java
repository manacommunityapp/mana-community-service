package com.manacommunity.api.safety.service.impl;

import com.manacommunity.api.safety.dto.AnprEventResponse;
import com.manacommunity.api.safety.dto.AnprSummaryResponse;
import com.manacommunity.api.safety.model.AnprEvent;
import com.manacommunity.api.safety.repository.AnprEventRepository;
import com.manacommunity.api.safety.service.AnprEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnprEventServiceImpl implements AnprEventService {

    private final AnprEventRepository anprEventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AnprEventResponse> getEvents(Long communityId, String gate, AnprEvent.Direction direction) {
        List<AnprEvent> events;
        if (gate != null && direction != null) {
            events = anprEventRepository.findByCommunityIdAndGateAndDirectionOrderByTimestampDesc(communityId, gate, direction);
        } else if (gate != null) {
            events = anprEventRepository.findByCommunityIdAndGateOrderByTimestampDesc(communityId, gate);
        } else if (direction != null) {
            events = anprEventRepository.findByCommunityIdAndDirectionOrderByTimestampDesc(communityId, direction);
        } else {
            events = anprEventRepository.findByCommunityIdOrderByTimestampDesc(communityId);
        }
        return events.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnprEventResponse getEventById(Long id) {
        AnprEvent event = anprEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ANPR event not found with id: " + id));
        return toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public AnprSummaryResponse getSummary(Long communityId) {
        List<AnprEvent> allEvents = anprEventRepository.findByCommunityIdOrderByTimestampDesc(communityId);

        AnprSummaryResponse summary = new AnprSummaryResponse();
        summary.setTotalEvents(allEvents.size());
        summary.setTotalEntries(allEvents.stream()
                .filter(e -> e.getDirection() == AnprEvent.Direction.ENTRY).count());
        summary.setTotalExits(allEvents.stream()
                .filter(e -> e.getDirection() == AnprEvent.Direction.EXIT).count());
        summary.setRecognizedCount(allEvents.stream()
                .filter(AnprEvent::getRecognized).count());
        summary.setUnrecognizedCount(allEvents.stream()
                .filter(e -> !e.getRecognized()).count());
        summary.setAlertCount(allEvents.stream()
                .filter(e -> e.getAlertType() != null && !e.getAlertType().isEmpty()).count());
        return summary;
    }

    private AnprEventResponse toResponse(AnprEvent event) {
        AnprEventResponse response = new AnprEventResponse();
        response.setId(event.getId());
        response.setPlateNumber(event.getPlateNumber());
        response.setVehicleType(event.getVehicleType());
        response.setGate(event.getGate());
        response.setDirection(event.getDirection().name());
        response.setTimestamp(event.getTimestamp());
        response.setImageUrl(event.getImageUrl());
        response.setRecognized(event.getRecognized());
        response.setOwnerName(event.getOwnerName());
        response.setOwnerFlat(event.getOwnerFlat());
        response.setAlertType(event.getAlertType());
        response.setCreatedAt(event.getCreatedAt());
        return response;
    }
}
