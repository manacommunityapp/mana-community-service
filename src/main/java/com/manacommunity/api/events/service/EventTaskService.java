package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventTaskRequest;
import com.manacommunity.api.events.dto.EventTaskResponse;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventTask;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventTaskRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
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
    private final EventCommunityRepository eventRepo;
    private final AppUserRepository userRepo;

    @Transactional(readOnly = true)
    public List<EventTaskResponse> getByEvent(Long eventId) {
        return taskRepo.findByEventIdOrderByDueDateAsc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventTaskResponse> getByCommunity(Long communityId) {
        List<EventTask> tasks;
        if (communityId != null) {
            tasks = taskRepo.findByCommunityIdOrderByDueDateAsc(communityId);
            if (tasks.isEmpty()) {
                tasks = taskRepo.findByEventCommunityIdOrderByCreatedAtDesc(communityId);
            }
        } else {
            tasks = taskRepo.findAll();
        }
        if (tasks == null || tasks.isEmpty()) {
            tasks = taskRepo.findAll();
        }
        return tasks.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventTaskResponse create(EventTaskRequest req, AppUser user, Community community) {
        EventCommunity event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", req.getEventId()));

        EventTask task = EventTask.builder()
                .event(event)
                .title(req.getTitle())
                .description(req.getDescription())
                .phase(req.getPhase())
                .priority(parseEnumOrDefault(EventTask.Priority.class, req.getPriority(), EventTask.Priority.MEDIUM))
                .assigneeName(req.getAssigneeName())
                .assignee(req.getAssigneeId() != null ? userRepo.findById(req.getAssigneeId()).orElse(null) : null)
                .dueDate(req.getDueDate())
                .community(community)
                .createdBy(user)
                .build();
        return toResponse(taskRepo.save(task));
    }

    @Transactional
    public EventTaskResponse update(Long id, EventTaskRequest req) {
        EventTask task = taskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setPhase(req.getPhase());
        task.setPriority(parseEnumOrDefault(EventTask.Priority.class, req.getPriority(), task.getPriority()));
        task.setAssigneeName(req.getAssigneeName());
        task.setAssignee(req.getAssigneeId() != null ? userRepo.findById(req.getAssigneeId()).orElse(null) : null);
        task.setDueDate(req.getDueDate());
        return toResponse(taskRepo.save(task));
    }

    @Transactional
    public EventTaskResponse toggleDone(Long id) {
        EventTask task = taskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
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
