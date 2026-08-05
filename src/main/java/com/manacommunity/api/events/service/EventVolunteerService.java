package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventVolunteerRequest;
import com.manacommunity.api.events.dto.EventVolunteerResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventVolunteer;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventVolunteerRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventVolunteerService {

    private final EventVolunteerRepository volunteerRepo;
    private final CommunityEventRepository eventRepo;
    private final AppUserRepository userRepo;

    @Transactional(readOnly = true)
    public List<EventVolunteerResponse> getByEvent(Long eventId) {
        return volunteerRepo.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventVolunteerResponse> getByCommunity(Long communityId) {
        return volunteerRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventVolunteerResponse create(EventVolunteerRequest req, Community community) {
        CommunityEvent event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + req.getEventId()));
        if (volunteerRepo.existsByEventIdAndUserId(req.getEventId(), req.getUserId())) {
            throw new IllegalStateException("User is already a volunteer for this event");
        }
        AppUser volunteer = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.getUserId()));

        EventVolunteer v = EventVolunteer.builder()
                .event(event)
                .user(volunteer)
                .role(req.getRole())
                .zone(req.getZone())
                .shift(req.getShift())
                .community(community)
                .build();
        return toResponse(volunteerRepo.save(v));
    }

    @Transactional
    public EventVolunteerResponse update(Long id, EventVolunteerRequest req) {
        EventVolunteer v = volunteerRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Volunteer not found: " + id));
        v.setRole(req.getRole());
        v.setZone(req.getZone());
        v.setShift(req.getShift());
        return toResponse(volunteerRepo.save(v));
    }

    @Transactional
    public EventVolunteerResponse checkIn(Long id) {
        EventVolunteer v = volunteerRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Volunteer not found: " + id));
        v.setStatus(EventVolunteer.VolunteerStatus.CHECKED_IN);
        v.setCheckInTime(LocalDateTime.now());
        return toResponse(volunteerRepo.save(v));
    }

    @Transactional
    public EventVolunteerResponse checkOut(Long id) {
        EventVolunteer v = volunteerRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Volunteer not found: " + id));
        v.setStatus(EventVolunteer.VolunteerStatus.CHECKED_OUT);
        v.setCheckOutTime(LocalDateTime.now());
        return toResponse(volunteerRepo.save(v));
    }

    @Transactional
    public void delete(Long id) {
        volunteerRepo.deleteById(id);
    }

    private EventVolunteerResponse toResponse(EventVolunteer v) {
        return EventVolunteerResponse.builder()
                .id(v.getId())
                .eventId(v.getEvent().getId())
                .eventTitle(v.getEvent().getTitle())
                .userId(v.getUser().getId())
                .userName(v.getUser().getFullName())
                .role(v.getRole())
                .zone(v.getZone())
                .shift(v.getShift())
                .status(v.getStatus().name())
                .checkInTime(formatDt(v.getCheckInTime()))
                .checkOutTime(formatDt(v.getCheckOutTime()))
                .createdAt(formatDt(v.getCreatedAt()))
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
