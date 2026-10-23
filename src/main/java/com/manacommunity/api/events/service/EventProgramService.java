package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventProgramRequest;
import com.manacommunity.api.events.dto.EventProgramResponse;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventProgram;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventProgramRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
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
public class EventProgramService {

    private final EventProgramRepository programRepo;
    private final EventCommunityRepository eventRepo;

    @Transactional(readOnly = true)
    public List<EventProgramResponse> getByEvent(Long eventId, String dayLabel) {
        if (dayLabel != null && !dayLabel.isBlank()) {
            return programRepo.findByEventIdAndDayLabelOrderBySortOrderAscStartTimeAsc(eventId, dayLabel)
                    .stream().map(this::toResponse).toList();
        }
        return programRepo.findByEventIdOrderBySortOrderAscStartTimeAsc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventProgramResponse create(EventProgramRequest req, AppUser user, Community community) {
        EventCommunity event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", req.getEventId()));

        EventProgram program = EventProgram.builder()
                .event(event)
                .title(req.getTitle())
                .dayLabel(req.getDayLabel())
                .dayDate(req.getDayDate() != null ? LocalDate.parse(req.getDayDate()) : null)
                .programType(req.getProgramType())
                .startTime(req.getStartTime() != null ? LocalTime.parse(req.getStartTime()) : null)
                .duration(req.getDuration())
                .venue(req.getVenue())
                .performer(req.getPerformer())
                .judge(req.getJudge())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .capacity(req.getCapacity())
                .requiresRegistration(Boolean.TRUE.equals(req.getRequiresRegistration()))
                .requiresApproval(Boolean.TRUE.equals(req.getRequiresApproval()))
                .allowWaitlist(req.getAllowWaitlist() == null || Boolean.TRUE.equals(req.getAllowWaitlist()))
                .maxPerRegistration(req.getMaxPerRegistration() != null && req.getMaxPerRegistration() > 0 ? req.getMaxPerRegistration() : 10)
                .community(community)
                .createdBy(user)
                .build();
        return toResponse(programRepo.save(program));
    }

    @Transactional
    public EventProgramResponse update(Long id, EventProgramRequest req) {
        EventProgram program = programRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program", id));
        program.setTitle(req.getTitle());
        program.setDayLabel(req.getDayLabel());
        program.setDayDate(req.getDayDate() != null ? LocalDate.parse(req.getDayDate()) : null);
        program.setProgramType(req.getProgramType());
        program.setStartTime(req.getStartTime() != null ? LocalTime.parse(req.getStartTime()) : null);
        program.setDuration(req.getDuration());
        program.setVenue(req.getVenue());
        program.setPerformer(req.getPerformer());
        program.setJudge(req.getJudge());
        program.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : program.getSortOrder());
        program.setCapacity(req.getCapacity());
        if (req.getRequiresRegistration() != null) program.setRequiresRegistration(req.getRequiresRegistration());
        if (req.getRequiresApproval() != null) program.setRequiresApproval(req.getRequiresApproval());
        if (req.getAllowWaitlist() != null) program.setAllowWaitlist(req.getAllowWaitlist());
        if (req.getMaxPerRegistration() != null && req.getMaxPerRegistration() > 0) program.setMaxPerRegistration(req.getMaxPerRegistration());
        return toResponse(programRepo.save(program));
    }

    @Transactional
    public void delete(Long id) {
        programRepo.deleteById(id);
    }

    private EventProgramResponse toResponse(EventProgram p) {
        return EventProgramResponse.builder()
                .id(p.getId())
                .eventId(p.getEvent().getId())
                .eventTitle(p.getEvent().getTitle())
                .dayLabel(p.getDayLabel())
                .dayDate(p.getDayDate() != null ? p.getDayDate().toString() : null)
                .title(p.getTitle())
                .programType(p.getProgramType())
                .startTime(p.getStartTime() != null ? p.getStartTime().toString() : null)
                .duration(p.getDuration())
                .venue(p.getVenue())
                .performer(p.getPerformer())
                .judge(p.getJudge())
                .sortOrder(p.getSortOrder())
                .capacity(p.getCapacity())
                .requiresRegistration(p.isRequiresRegistration())
                .requiresApproval(p.isRequiresApproval())
                .allowWaitlist(p.isAllowWaitlist())
                .maxPerRegistration(p.getMaxPerRegistration())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }
}
