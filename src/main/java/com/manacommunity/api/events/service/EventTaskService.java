package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventTaskRequest;
import com.manacommunity.api.events.dto.EventTaskResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventTask;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventTaskRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventTaskService {

    private final EventTaskRepository taskRepo;
    private final CommunityEventRepository eventRepo;
    private final AppUserRepository userRepo;

    @Transactional(readOnly = true)
    public List<EventTaskResponse> getByEvent(Long eventId) {
        return taskRepo.findByEventIdOrderByDueDateAsc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventTaskResponse> getByCommunity(Long communityId) {
        return taskRepo.findByCommunityIdOrderByDueDateAsc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventTaskResponse create(EventTaskRequest req, AppUser user, Community community) {
        CommunityEvent event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + req.getEventId()));

        EventTask task = EventTask.builder()
                .event(event)
                .title(req.getTitle())
                .description(req.getDescription())
                .phase(req.getPhase())
                .priority(parseEnumOrDefault(EventTask.Priority.class, req.getPriority(), EventTask.Priority.MEDIUM))
                .assigneeName(req.getAssigneeName())
                .assignee(req.getAssigneeId() != null ? userRepo.findById(req.getAssigneeId()).orElse(null) : null)
                .dueDate(req.getDueDate() != null ? LocalDate.parse(req.getDueDate()) : null)
                .community(community)
                .createdBy(user)
                .build();
        return toResponse(taskRepo.save(task));
    }

    @Transactional
    public EventTaskResponse update(Long id, EventTaskRequest req) {
        EventTask task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setPhase(req.getPhase());
        task.setPriority(parseEnumOrDefault(EventTask.Priority.class, req.getPriority(), task.getPriority()));
        task.setAssigneeName(req.getAssigneeName());
        task.setAssignee(req.getAssigneeId() != null ? userRepo.findById(req.getAssigneeId()).orElse(null) : null);
        task.setDueDate(req.getDueDate() != null ? LocalDate.parse(req.getDueDate()) : null);
        return toResponse(taskRepo.save(task));
    }

    @Transactional
    public EventTaskResponse toggleDone(Long id) {
        EventTask task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        task.setDone(!task.isDone());
        return toResponse(taskRepo.save(task));
    }

    @Transactional
    public void delete(Long id) {
        taskRepo.deleteById(id);
    }

    private EventTaskResponse toResponse(EventTask t) {
        return EventTaskResponse.builder()
                .id(t.getId())
                .eventId(t.getEvent().getId())
                .eventTitle(t.getEvent().getTitle())
                .title(t.getTitle())
                .description(t.getDescription())
                .phase(t.getPhase())
                .priority(t.getPriority().name())
                .assigneeName(t.getAssigneeName())
                .assigneeId(t.getAssignee() != null ? t.getAssignee().getId() : null)
                .dueDate(t.getDueDate() != null ? t.getDueDate().toString() : null)
                .done(t.isDone())
                .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        if (value == null || value.isBlank()) return def;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return def; }
    }
}
