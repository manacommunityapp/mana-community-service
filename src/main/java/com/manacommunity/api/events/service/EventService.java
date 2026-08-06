package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.DashboardStatsResponse;
import com.manacommunity.api.events.dto.EventRequest;
import com.manacommunity.api.events.dto.EventResponse;
import com.manacommunity.api.events.dto.RegistrationResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventRegistration;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final CommunityEventRepository eventRepo;
    private final EventRegistrationRepository regRepo;

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents(Long communityId, String typeFilter, Long currentUserId) {
        List<CommunityEvent> events;
        if (typeFilter != null && !typeFilter.isBlank() && !"All".equalsIgnoreCase(typeFilter)) {
            CommunityEvent.EventType type = parseEnum(CommunityEvent.EventType.class, typeFilter);
            if (type != null) {
                events = eventRepo.findUpcomingByCommunityAndType(communityId, type);
            } else {
                events = eventRepo.findUpcomingByCommunity(communityId);
            }
        } else {
            events = eventRepo.findUpcomingByCommunity(communityId);
        }
        return events.stream().map(e -> toResponse(e, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(Long communityId, Long currentUserId) {
        return eventRepo.findByCommunityIdOrderByStartDateDesc(communityId)
                .stream().map(e -> toResponse(e, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents(Long userId) {
        List<Long> eventIds = regRepo.findByUserIdOrderByRegisteredAtDesc(userId)
                .stream().map(r -> r.getEvent().getId()).toList();
        return eventIds.stream()
                .map(id -> eventRepo.findById(id).orElse(null))
                .filter(e -> e != null)
                .map(e -> toResponse(e, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getById(Long id, Long currentUserId) {
        CommunityEvent event = eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
        return toResponse(event, currentUserId);
    }

    @Transactional
    public EventResponse create(EventRequest req, AppUser user, Community community) {
        CommunityEvent event = CommunityEvent.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .type(parseEnumOrDefault(CommunityEvent.EventType.class, req.getType(), CommunityEvent.EventType.GENERAL))
                .startDate(LocalDate.parse(req.getStartDate()))
                .endDate(req.getEndDate() != null ? LocalDate.parse(req.getEndDate()) : null)
                .startTime(req.getStartTime() != null ? LocalTime.parse(req.getStartTime()) : null)
                .endTime(req.getEndTime() != null ? LocalTime.parse(req.getEndTime()) : null)
                .locationType(parseEnumOrDefault(CommunityEvent.LocationType.class, req.getLocationType(), CommunityEvent.LocationType.IN_PERSON))
                .location(req.getLocation())
                .priceType(parseEnumOrDefault(CommunityEvent.PriceType.class, req.getPriceType(), CommunityEvent.PriceType.FREE))
                .price(req.getPrice())
                .capacity(req.getCapacity())
                .imageUrl(req.getImageUrl())
                .organizerName(req.getOrganizerName())
                .organizerContact(req.getOrganizerContact())
                .createdBy(user)
                .community(community)
                .build();
        return toResponse(eventRepo.save(event), user.getId());
    }

    @Transactional
    public EventResponse update(Long id, EventRequest req, Long userId) {
        CommunityEvent event = eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
        if (!event.getCreatedBy().getId().equals(userId)) {
            throw new IllegalStateException("Only the creator can edit this event");
        }
        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setType(parseEnumOrDefault(CommunityEvent.EventType.class, req.getType(), event.getType()));
        event.setStartDate(LocalDate.parse(req.getStartDate()));
        event.setEndDate(req.getEndDate() != null ? LocalDate.parse(req.getEndDate()) : null);
        event.setStartTime(req.getStartTime() != null ? LocalTime.parse(req.getStartTime()) : null);
        event.setEndTime(req.getEndTime() != null ? LocalTime.parse(req.getEndTime()) : null);
        event.setLocationType(parseEnumOrDefault(CommunityEvent.LocationType.class, req.getLocationType(), event.getLocationType()));
        event.setLocation(req.getLocation());
        event.setPriceType(parseEnumOrDefault(CommunityEvent.PriceType.class, req.getPriceType(), event.getPriceType()));
        event.setPrice(req.getPrice());
        event.setCapacity(req.getCapacity());
        event.setImageUrl(req.getImageUrl());
        event.setOrganizerName(req.getOrganizerName());
        event.setOrganizerContact(req.getOrganizerContact());
        return toResponse(eventRepo.save(event), userId);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        CommunityEvent event = eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
        if (!event.getCreatedBy().getId().equals(userId)) {
            throw new IllegalStateException("Only the creator can delete this event");
        }
        eventRepo.delete(event);
    }

    @Transactional
    public EventResponse register(Long eventId, AppUser user) {
        CommunityEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        if (regRepo.existsByEventIdAndUserId(eventId, user.getId())) {
            throw new IllegalStateException("Already registered for this event");
        }
        if (event.getCapacity() != null && event.getRegistrations().size() >= event.getCapacity()) {
            throw new IllegalStateException("Event is at full capacity");
        }
        EventRegistration reg = EventRegistration.builder()
                .event(event)
                .user(user)
                .build();
        regRepo.save(reg);
        return toResponse(eventRepo.findById(eventId).orElseThrow(), user.getId());
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Long communityId) {
        return DashboardStatsResponse.builder()
                .totalEvents(eventRepo.countByCommunityId(communityId))
                .upcomingEvents(eventRepo.countUpcomingByCommunity(communityId))
                .totalRegistrations(regRepo.countByEventCommunityId(communityId))
                .build();
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getEventRegistrations(Long eventId) {
        return regRepo.findByEventId(eventId).stream()
                .map(this::toRegistrationResponse)
                .toList();
    }

    @Transactional
    public RegistrationResponse confirmRegistration(Long registrationId) {
        EventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + registrationId));
        reg.setStatus(EventRegistration.RegistrationStatus.CONFIRMED);
        return toRegistrationResponse(regRepo.save(reg));
    }

    @Transactional
    public RegistrationResponse rejectRegistration(Long registrationId) {
        EventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + registrationId));
        reg.setStatus(EventRegistration.RegistrationStatus.REJECTED);
        return toRegistrationResponse(regRepo.save(reg));
    }

    @Transactional
    public EventResponse unregister(Long eventId, Long userId) {
        EventRegistration reg = regRepo.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new IllegalStateException("Not registered for this event"));
        regRepo.delete(reg);
        return toResponse(eventRepo.findById(eventId).orElseThrow(), userId);
    }

    private RegistrationResponse toRegistrationResponse(EventRegistration r) {
        return RegistrationResponse.builder()
                .id(r.getId())
                .eventId(r.getEvent().getId())
                .eventTitle(r.getEvent().getTitle())
                .userId(r.getUser().getId())
                .userName(r.getUser().getFullName())
                .userEmail(r.getUser().getEmail())
                .status(r.getStatus().name())
                .registeredAt(formatDt(r.getRegisteredAt()))
                .build();
    }

    private EventResponse toResponse(CommunityEvent e, Long currentUserId) {
        boolean isRegistered = currentUserId != null && regRepo.existsByEventIdAndUserId(e.getId(), currentUserId);
        return EventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .type(e.getType().name())
                .startDate(e.getStartDate().toString())
                .endDate(e.getEndDate() != null ? e.getEndDate().toString() : null)
                .startTime(e.getStartTime() != null ? e.getStartTime().toString() : null)
                .endTime(e.getEndTime() != null ? e.getEndTime().toString() : null)
                .locationType(e.getLocationType().name())
                .location(e.getLocation())
                .priceType(e.getPriceType().name())
                .price(e.getPrice())
                .capacity(e.getCapacity())
                .imageUrl(e.getImageUrl())
                .organizerName(e.getOrganizerName())
                .organizerContact(e.getOrganizerContact())
                .createdById(e.getCreatedBy().getId())
                .createdByName(e.getCreatedBy().getFullName())
                .communityId(e.getCommunity() != null ? e.getCommunity().getId() : null)
                .attendees(e.getRegistrations() != null ? e.getRegistrations().size() : 0)
                .isRegistered(isRegistered)
                .createdAt(formatDt(e.getCreatedAt()))
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return null; }
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        E r = parseEnum(enumClass, value);
        return r != null ? r : def;
    }
}
