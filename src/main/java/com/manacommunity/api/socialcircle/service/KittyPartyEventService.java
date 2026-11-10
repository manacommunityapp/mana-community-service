package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.socialcircle.dto.KittyPartyEventDtos.*;
import com.manacommunity.api.socialcircle.entity.KittyPartyCircle;
import com.manacommunity.api.socialcircle.entity.KittyPartyEvent;
import com.manacommunity.api.socialcircle.entity.KittyPartyEventAttendance;
import com.manacommunity.api.socialcircle.entity.KittyPartyEventRsvp;
import com.manacommunity.api.socialcircle.repository.KittyPartyCircleRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyEventAttendanceRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyEventRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyEventRsvpRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KittyPartyEventService {

    private final KittyPartyEventRepository eventRepository;
    private final KittyPartyEventRsvpRepository rsvpRepository;
    private final KittyPartyEventAttendanceRepository attendanceRepository;
    private final KittyPartyCircleRepository circleRepository;
    private final AppUserRepository userRepository;
    private final KittyPartyAuthorizationService authorizationService;

    @Transactional
    public EventResponse createEvent(AppUser creator, Long circleId, CreateEventRequest request) {
        authorizationService.assertEventManager(circleId, creator.getId());

        KittyPartyCircle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found with id: " + circleId));

        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new IllegalArgumentException("Event end time must be after start time");
        }

        AppUser host = null;
        if (request.getHostUserId() != null) {
            host = userRepository.findById(request.getHostUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Host user not found with id: " + request.getHostUserId()));
        } else {
            host = creator;
        }

        KittyPartyEvent event = KittyPartyEvent.builder()
                .circle(circle)
                .eventName(request.getEventName().trim())
                .description(request.getDescription())
                .eventType(request.getEventType() != null ? request.getEventType() : "KITTY_MONTHLY")
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "Asia/Kolkata")
                .venueType(request.getVenueType() != null ? request.getVenueType() : "HOST_HOME")
                .venueAddress(request.getVenueAddress())
                .hostUser(host)
                .maxAttendees(request.getMaxAttendees())
                .recurrenceRule(request.getRecurrenceRule() != null ? request.getRecurrenceRule() : "NONE")
                .status("SCHEDULED")
                .createdBy(creator)
                .build();

        KittyPartyEvent saved = eventRepository.save(event);
        log.info("Created kitty party event id={} circleId={} name='{}'", saved.getId(), circleId, saved.getEventName());

        // Automatically mark creator/host as GOING
        rsvpRepository.save(KittyPartyEventRsvp.builder()
                .event(saved)
                .user(creator)
                .response("GOING")
                .guestCount(0)
                .notes("Event Host")
                .gatePassCode("GP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .gatePassGeneratedAt(LocalDateTime.now())
                .build());

        return toEventResponse(saved, creator.getId());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByCircle(Long circleId, Long currentUserId) {
        if (currentUserId != null) {
            authorizationService.assertCircleMember(circleId, currentUserId);
        }
        return eventRepository.findByCircleIdOrderByStartTimeAsc(circleId).stream()
                .map(e -> toEventResponse(e, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents(Long circleId, Long currentUserId) {
        if (currentUserId != null) {
            authorizationService.assertCircleMember(circleId, currentUserId);
        }
        return eventRepository.findUpcomingEvents(circleId, LocalDateTime.now()).stream()
                .map(e -> toEventResponse(e, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId, Long currentUserId) {
        KittyPartyEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
        return toEventResponse(event, currentUserId);
    }

    @Transactional
    public EventResponse updateEvent(AppUser actor, Long eventId, UpdateEventRequest request) {
        if (actor == null) {
            throw new SecurityException("Authentication required to update events");
        }
        KittyPartyEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
        authorizationService.assertEventManager(event.getCircle().getId(), actor.getId());

        if (request.getEventName() != null && !request.getEventName().isBlank()) {
            event.setEventName(request.getEventName().trim());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventType() != null) {
            event.setEventType(request.getEventType());
        }
        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            event.setEndTime(request.getEndTime());
        }
        if (event.getEndTime().isBefore(event.getStartTime()) || event.getEndTime().isEqual(event.getStartTime())) {
            throw new IllegalArgumentException("Event end time must be after start time");
        }
        if (request.getVenueType() != null) {
            event.setVenueType(request.getVenueType());
        }
        if (request.getVenueAddress() != null) {
            event.setVenueAddress(request.getVenueAddress());
        }
        if (request.getHostUserId() != null) {
            AppUser host = userRepository.findById(request.getHostUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Host user not found"));
            event.setHostUser(host);
        }
        if (request.getMaxAttendees() != null) {
            event.setMaxAttendees(request.getMaxAttendees());
        }
        if (request.getRecurrenceRule() != null) {
            event.setRecurrenceRule(request.getRecurrenceRule());
        }
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }
        event.setUpdatedAt(LocalDateTime.now());

        KittyPartyEvent updated = eventRepository.save(event);
        log.info("Updated kitty party event id={}", eventId);
        return toEventResponse(updated, actor != null ? actor.getId() : null);
    }

    @Transactional
    public RsvpResponse submitRsvp(AppUser user, Long eventId, RsvpRequest request) {
        KittyPartyEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
        authorizationService.assertCircleMember(event.getCircle().getId(), user.getId());

        if ("CANCELLED".equalsIgnoreCase(event.getStatus())) {
            throw new IllegalStateException("Cannot RSVP to a cancelled event");
        }

        String responseVal = request.getResponse().toUpperCase();
        if (event.getMaxAttendees() != null && "GOING".equals(responseVal)) {
            long currentGoing = rsvpRepository.countByEventIdAndResponse(eventId, "GOING");
            int guests = request.getGuestCount() != null ? request.getGuestCount() : 0;
            if (currentGoing + 1 + guests > event.getMaxAttendees()) {
                responseVal = "WAITLISTED";
                log.info("Event id={} is full; setting user id={} RSVP to WAITLISTED", eventId, user.getId());
            }
        }

        Optional<KittyPartyEventRsvp> existingOpt = rsvpRepository.findByEventIdAndUserId(eventId, user.getId());
        KittyPartyEventRsvp rsvp;
        if (existingOpt.isPresent()) {
            rsvp = existingOpt.get();
            rsvp.setResponse(responseVal);
            rsvp.setGuestCount(request.getGuestCount() != null ? request.getGuestCount() : 0);
            rsvp.setNotes(request.getNotes());
            rsvp.setUpdatedAt(LocalDateTime.now());
        } else {
            rsvp = KittyPartyEventRsvp.builder()
                    .event(event)
                    .user(user)
                    .response(responseVal)
                    .guestCount(request.getGuestCount() != null ? request.getGuestCount() : 0)
                    .notes(request.getNotes())
                    .build();
        }

        if ("GOING".equals(responseVal)) {
            if (rsvp.getGatePassCode() == null) {
                rsvp.setGatePassCode("GP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
                rsvp.setGatePassGeneratedAt(LocalDateTime.now());
            }
        } else {
            rsvp.setGatePassCode(null);
            rsvp.setGatePassGeneratedAt(null);
        }

        KittyPartyEventRsvp saved = rsvpRepository.save(rsvp);
        log.info("User id={} RSVP'd {} for event id={}", user.getId(), responseVal, eventId);
        return toRsvpResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RsvpResponse> getEventRsvps(Long eventId) {
        return rsvpRepository.findByEventId(eventId).stream()
                .map(this::toRsvpResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AttendanceResponse markAttendance(AppUser markedBy, Long eventId, MarkAttendanceRequest request) {
        KittyPartyEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
        authorizationService.assertEventManager(event.getCircle().getId(), markedBy.getId());

        AppUser attendee = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Attendee user not found with id: " + request.getUserId()));

        Optional<KittyPartyEventAttendance> existingOpt = attendanceRepository.findByEventIdAndUserId(eventId, attendee.getId());
        KittyPartyEventAttendance attendance;
        if (existingOpt.isPresent()) {
            attendance = existingOpt.get();
            attendance.setAttendanceStatus(request.getAttendanceStatus().toUpperCase());
            attendance.setMarkedBy(markedBy);
            attendance.setMarkedAt(LocalDateTime.now());
        } else {
            attendance = KittyPartyEventAttendance.builder()
                    .event(event)
                    .user(attendee)
                    .attendanceStatus(request.getAttendanceStatus().toUpperCase())
                    .markedBy(markedBy)
                    .markedAt(LocalDateTime.now())
                    .build();
        }

        KittyPartyEventAttendance saved = attendanceRepository.save(attendance);
        log.info("Marked attendance for user id={} as {} in event id={}", attendee.getId(), saved.getAttendanceStatus(), eventId);
        return toAttendanceResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getEventAttendance(Long eventId) {
        return attendanceRepository.findByEventId(eventId).stream()
                .map(this::toAttendanceResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventSummaryDto getEventSummary(Long circleId, Long currentUserId) {
        if (currentUserId != null) {
            authorizationService.assertCircleMember(circleId, currentUserId);
        }
        List<KittyPartyEvent> allEvents = eventRepository.findByCircleIdOrderByStartTimeAsc(circleId);
        LocalDateTime now = LocalDateTime.now();
        long upcoming = allEvents.stream().filter(e -> !e.getEndTime().isBefore(now)).count();
        long past = allEvents.size() - upcoming;

        List<EventResponse> dtos = allEvents.stream()
                .map(e -> toEventResponse(e, currentUserId))
                .collect(Collectors.toList());

        return EventSummaryDto.builder()
                .totalEvents(allEvents.size())
                .upcomingEvents(upcoming)
                .pastEvents(past)
                .events(dtos)
                .build();
    }

    private EventResponse toEventResponse(KittyPartyEvent e, Long currentUserId) {
        long going = rsvpRepository.countByEventIdAndResponse(e.getId(), "GOING");
        long maybe = rsvpRepository.countByEventIdAndResponse(e.getId(), "MAYBE");
        long notGoing = rsvpRepository.countByEventIdAndResponse(e.getId(), "NOT_GOING");

        String userRsvp = null;
        String gatePassCode = null;
        LocalDateTime gatePassGeneratedAt = null;
        if (currentUserId != null) {
            Optional<KittyPartyEventRsvp> rsvpOpt = rsvpRepository.findByEventIdAndUserId(e.getId(), currentUserId);
            if (rsvpOpt.isPresent()) {
                userRsvp = rsvpOpt.get().getResponse();
                gatePassCode = rsvpOpt.get().getGatePassCode();
                gatePassGeneratedAt = rsvpOpt.get().getGatePassGeneratedAt();
            }
        }

        return EventResponse.builder()
                .id(e.getId())
                .circleId(e.getCircle().getId())
                .circleName(e.getCircle().getCircleName())
                .eventName(e.getEventName())
                .description(e.getDescription())
                .eventType(e.getEventType())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .timezone(e.getTimezone())
                .venueType(e.getVenueType())
                .venueAddress(e.getVenueAddress())
                .hostUserId(e.getHostUser() != null ? e.getHostUser().getId() : null)
                .hostName(e.getHostUser() != null ? e.getHostUser().getFullName() : null)
                .maxAttendees(e.getMaxAttendees())
                .recurrenceRule(e.getRecurrenceRule())
                .status(e.getStatus())
                .goingCount(going)
                .maybeCount(maybe)
                .notGoingCount(notGoing)
                .currentUserRsvp(userRsvp)
                .gatePassCode(gatePassCode)
                .gatePassGeneratedAt(gatePassGeneratedAt)
                .createdById(e.getCreatedBy() != null ? e.getCreatedBy().getId() : null)
                .createdByName(e.getCreatedBy() != null ? e.getCreatedBy().getFullName() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private RsvpResponse toRsvpResponse(KittyPartyEventRsvp r) {
        return RsvpResponse.builder()
                .id(r.getId())
                .eventId(r.getEvent().getId())
                .userId(r.getUser().getId())
                .userName(r.getUser().getFullName())
                .response(r.getResponse())
                .guestCount(r.getGuestCount())
                .notes(r.getNotes())
                .gatePassCode(r.getGatePassCode())
                .gatePassGeneratedAt(r.getGatePassGeneratedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private AttendanceResponse toAttendanceResponse(KittyPartyEventAttendance a) {
        return AttendanceResponse.builder()
                .id(a.getId())
                .eventId(a.getEvent().getId())
                .userId(a.getUser().getId())
                .userName(a.getUser().getFullName())
                .attendanceStatus(a.getAttendanceStatus())
                .markedById(a.getMarkedBy() != null ? a.getMarkedBy().getId() : null)
                .markedByName(a.getMarkedBy() != null ? a.getMarkedBy().getFullName() : null)
                .markedAt(a.getMarkedAt())
                .build();
    }
}
