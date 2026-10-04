package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.DiningEventRequest;
import com.manacommunity.api.dto.DiningEventResponse;
import com.manacommunity.api.dto.DiningRsvpRequest;
import com.manacommunity.api.dto.DiningRsvpResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.DiningEvent;
import com.manacommunity.api.model.DiningEvent.DiningEventStatus;
import com.manacommunity.api.model.DiningRsvp;
import com.manacommunity.api.model.DiningRsvp.RsvpStatus;
import com.manacommunity.api.repository.DiningEventRepository;
import com.manacommunity.api.repository.DiningRsvpRepository;
import com.manacommunity.api.service.DiningService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiningServiceImpl implements DiningService {

    private final DiningEventRepository eventRepository;
    private final DiningRsvpRepository rsvpRepository;

    @Override
    public List<DiningEventResponse> getCommunityEvents(Long communityId) {
        return eventRepository.findByCommunityIdOrderByDateDescTimeDesc(communityId)
                .stream()
                .map(this::toEventResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DiningEventResponse getEvent(Long id, Long communityId) {
        DiningEvent event = eventRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DiningEvent", "id", id.toString()));
        return toEventResponse(event);
    }

    @Override
    @Transactional
    public DiningEventResponse createEvent(DiningEventRequest request, AppUser user) {
        DiningEvent event = DiningEvent.builder()
                .community(user.getCommunity())
                .title(request.getTitle())
                .description(request.getDescription())
                .date(request.getDate())
                .time(request.getTime())
                .venue(request.getVenue())
                .maxCapacity(request.getMaxCapacity())
                .currentRsvps(0)
                .pricePerPerson(request.getPricePerPerson())
                .menuDescription(request.getMenuDescription())
                .hostUser(user)
                .status(DiningEventStatus.UPCOMING)
                .build();

        return toEventResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public DiningRsvpResponse createRsvp(DiningRsvpRequest request, AppUser user) {
        Long communityId = user.getCommunity().getId();
        DiningEvent event = eventRepository.findByIdAndCommunityId(request.getDiningEventId(), communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DiningEvent", "id", request.getDiningEventId().toString()));

        if (rsvpRepository.existsByDiningEventIdAndUserId(event.getId(), user.getId())) {
            throw new IllegalStateException("You have already RSVP'd to this event.");
        }

        RsvpStatus status;
        if (event.getMaxCapacity() != null && event.getCurrentRsvps() + request.getGuestCount() > event.getMaxCapacity()) {
            status = RsvpStatus.WAITLISTED;
        } else {
            status = RsvpStatus.CONFIRMED;
            event.setCurrentRsvps(event.getCurrentRsvps() + request.getGuestCount());
            eventRepository.save(event);
        }

        DiningRsvp rsvp = DiningRsvp.builder()
                .diningEvent(event)
                .user(user)
                .guestCount(request.getGuestCount())
                .specialRequests(request.getSpecialRequests())
                .status(status)
                .build();

        return toRsvpResponse(rsvpRepository.save(rsvp));
    }

    @Override
    @Transactional
    public void cancelRsvp(Long rsvpId, Long userId) {
        DiningRsvp rsvp = rsvpRepository.findByIdAndUserId(rsvpId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("DiningRsvp", "id", rsvpId.toString()));

        if (rsvp.getStatus() == RsvpStatus.CONFIRMED) {
            DiningEvent event = rsvp.getDiningEvent();
            event.setCurrentRsvps(Math.max(0, event.getCurrentRsvps() - rsvp.getGuestCount()));
            eventRepository.save(event);
        }

        rsvp.setStatus(RsvpStatus.CANCELLED);
        rsvpRepository.save(rsvp);
    }

    private DiningEventResponse toEventResponse(DiningEvent event) {
        return DiningEventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .date(event.getDate())
                .time(event.getTime())
                .venue(event.getVenue())
                .maxCapacity(event.getMaxCapacity())
                .currentRsvps(event.getCurrentRsvps())
                .pricePerPerson(event.getPricePerPerson())
                .menuDescription(event.getMenuDescription())
                .hostUserId(event.getHostUser() != null ? event.getHostUser().getId() : null)
                .hostUserName(event.getHostUser() != null ? event.getHostUser().getFullName() : null)
                .status(event.getStatus().name())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private DiningRsvpResponse toRsvpResponse(DiningRsvp rsvp) {
        return DiningRsvpResponse.builder()
                .id(rsvp.getId())
                .diningEventId(rsvp.getDiningEvent().getId())
                .diningEventTitle(rsvp.getDiningEvent().getTitle())
                .userId(rsvp.getUser().getId())
                .userName(rsvp.getUser().getFullName())
                .guestCount(rsvp.getGuestCount())
                .specialRequests(rsvp.getSpecialRequests())
                .status(rsvp.getStatus().name())
                .createdAt(rsvp.getCreatedAt())
                .build();
    }
}
