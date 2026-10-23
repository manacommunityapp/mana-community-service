package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventTaskRequest;
import com.manacommunity.api.events.dto.EventTaskResponse;
import com.manacommunity.api.events.dto.EventVolunteerRequest;
import com.manacommunity.api.events.dto.EventVolunteerResponse;
import com.manacommunity.api.events.dto.EventSponsorRequest;
import com.manacommunity.api.events.dto.EventSponsorResponse;
import com.manacommunity.api.events.dto.EventExpenseRequest;
import com.manacommunity.api.events.dto.EventExpenseResponse;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventTask;
import com.manacommunity.api.events.entity.EventVolunteer;
import com.manacommunity.api.events.entity.EventSponsor;
import com.manacommunity.api.events.entity.EventExpense;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventTaskRepository;
import com.manacommunity.api.events.repository.EventVolunteerRepository;
import com.manacommunity.api.events.repository.EventSponsorRepository;
import com.manacommunity.api.events.repository.EventExpenseRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSubResourceService {

    private final EventCommunityRepository eventRepo;
    private final EventTaskRepository taskRepo;
    private final EventVolunteerRepository volunteerRepo;
    private final EventSponsorRepository sponsorRepo;
    private final EventExpenseRepository expenseRepo;

    // ── Helper ──────────────────────────────────────────────────────────────────
    private EventCommunity findEvent(Long eventId) {
        return eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String val, E def) {
        if (val == null || val.isBlank()) return def;
        try { return Enum.valueOf(cls, val.toUpperCase()); }
        catch (IllegalArgumentException e) { return def; }
    }

    // ── Tasks ────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventTaskResponse> getTasks(Long eventId) {
        List<EventTask> tasks = eventId != null
                ? taskRepo.findByEventIdOrderByCreatedAtDesc(eventId)
                : taskRepo.findAll();
        return tasks.stream().map(this::toTaskResponse).toList();
    }

    @Transactional
    public EventTaskResponse createTask(EventTaskRequest req) {
        EventCommunity event = findEvent(req.getEventId());
        EventTask task = EventTask.builder()
                .event(event)
                .title(req.getTitle())
                .description(req.getDescription())
                .phase(req.getPhase())
                .priority(parseEnum(EventTask.Priority.class, req.getPriority(), EventTask.Priority.MEDIUM))
                .assigneeName(req.getAssigneeName())
                .dueDate(req.getDueDate())
                .done(false)
                .build();
        return toTaskResponse(taskRepo.save(task));
    }

    @Transactional
    public EventTaskResponse updateTask(Long id, EventTaskRequest req) {
        EventTask task = taskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventTask", id));
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setPhase(req.getPhase());
        if (req.getPriority() != null) {
            task.setPriority(parseEnum(EventTask.Priority.class, req.getPriority(), task.getPriority()));
        }
        task.setAssigneeName(req.getAssigneeName());
        task.setDueDate(req.getDueDate());
        return toTaskResponse(taskRepo.save(task));
    }

    @Transactional
    public EventTaskResponse toggleDone(Long id) {
        EventTask task = taskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventTask", id));
        task.setDone(!task.isDone());
        return toTaskResponse(taskRepo.save(task));
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepo.deleteById(id);
    }

    private EventTaskResponse toTaskResponse(EventTask t) {
        return EventTaskResponse.builder()
                .id(t.getId())
                .eventId(t.getEvent().getId())
                .eventTitle(t.getEvent().getTitle())
                .title(t.getTitle())
                .description(t.getDescription())
                .phase(t.getPhase())
                .priority(t.getPriority() != null ? t.getPriority().name() : null)
                .assigneeName(t.getAssigneeName())
                .assigneeId(t.getAssignee() != null ? t.getAssignee().getId() : null)
                .dueDate(t.getDueDate() != null ? t.getDueDate().toString() : null)
                .done(t.isDone())
                .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
                .build();
    }

    // ── Volunteers ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventVolunteerResponse> getVolunteers(Long eventId) {
        List<EventVolunteer> vols = eventId != null
                ? volunteerRepo.findByEventIdOrderByCreatedAtDesc(eventId)
                : volunteerRepo.findAll();
        return vols.stream().map(this::toVolunteerResponse).toList();
    }

    @Transactional
    public EventVolunteerResponse createVolunteer(EventVolunteerRequest req) {
        EventCommunity event = findEvent(req.getEventId());
        EventVolunteer vol = EventVolunteer.builder()
                .event(event)
                .userId(req.getUserId())
                .userName(req.getUserName())
                .role(req.getRole())
                .zone(req.getZone())
                .shift(req.getShift())
                .status("ACTIVE")
                .build();
        return toVolunteerResponse(volunteerRepo.save(vol));
    }

    @Transactional
    public EventVolunteerResponse updateVolunteer(Long id, EventVolunteerRequest req) {
        EventVolunteer vol = volunteerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventVolunteer", id));
        vol.setRole(req.getRole());
        vol.setZone(req.getZone());
        vol.setShift(req.getShift());
        return toVolunteerResponse(volunteerRepo.save(vol));
    }

    @Transactional
    public EventVolunteerResponse checkIn(Long id) {
        EventVolunteer vol = volunteerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventVolunteer", id));
        vol.setCheckInTime(LocalDateTime.now());
        vol.setVolunteerStatus(EventVolunteer.VolunteerStatus.CHECKED_IN);
        return toVolunteerResponse(volunteerRepo.save(vol));
    }

    @Transactional
    public EventVolunteerResponse checkOut(Long id) {
        EventVolunteer vol = volunteerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventVolunteer", id));
        vol.setCheckOutTime(LocalDateTime.now());
        vol.setVolunteerStatus(EventVolunteer.VolunteerStatus.CHECKED_OUT);
        return toVolunteerResponse(volunteerRepo.save(vol));
    }

    @Transactional
    public void deleteVolunteer(Long id) {
        volunteerRepo.deleteById(id);
    }

    private EventVolunteerResponse toVolunteerResponse(EventVolunteer v) {
        String statusStr = v.getVolunteerStatus() != null
                ? v.getVolunteerStatus().name()
                : (v.getStatus() != null ? v.getStatus() : "ACTIVE");
        return EventVolunteerResponse.builder()
                .id(v.getId())
                .eventId(v.getEvent().getId())
                .eventTitle(v.getEvent().getTitle())
                .userId(v.getUserId())
                .userName(v.getUserName())
                .role(v.getRole())
                .zone(v.getZone())
                .shift(v.getShift())
                .status(statusStr)
                .checkInTime(v.getCheckInTime() != null ? v.getCheckInTime().toString() : null)
                .checkOutTime(v.getCheckOutTime() != null ? v.getCheckOutTime().toString() : null)
                .createdAt(v.getCreatedAt() != null ? v.getCreatedAt().toString() : null)
                .build();
    }

    // ── Sponsors ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventSponsorResponse> getSponsors(Long eventId) {
        List<EventSponsor> sponsors = eventId != null
                ? sponsorRepo.findByEventIdOrderByCreatedAtDesc(eventId)
                : sponsorRepo.findAll();
        return sponsors.stream().map(this::toSponsorResponse).toList();
    }

    @Transactional
    public EventSponsorResponse createSponsor(EventSponsorRequest req) {
        EventCommunity event = findEvent(req.getEventId());
        EventSponsor sponsor = EventSponsor.builder()
                .event(event)
                .name(req.getName())
                .tier(req.getTier() != null ? req.getTier() : "GENERAL")
                .amountPledged(req.getAmountPledged())
                .amountReceived(req.getAmountReceived())
                .logoUrl(req.getLogoUrl())
                .contactName(req.getContactName())
                .contactPhone(req.getContactPhone())
                .contactEmail(req.getContactEmail())
                .status(req.getStatus() != null ? req.getStatus() : "ACTIVE")
                .build();
        return toSponsorResponse(sponsorRepo.save(sponsor));
    }

    @Transactional
    public EventSponsorResponse updateSponsor(Long id, EventSponsorRequest req) {
        EventSponsor sponsor = sponsorRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventSponsor", id));
        sponsor.setName(req.getName());
        if (req.getTier() != null) sponsor.setTier(req.getTier());
        sponsor.setAmountPledged(req.getAmountPledged());
        sponsor.setAmountReceived(req.getAmountReceived());
        sponsor.setLogoUrl(req.getLogoUrl());
        sponsor.setContactName(req.getContactName());
        sponsor.setContactPhone(req.getContactPhone());
        sponsor.setContactEmail(req.getContactEmail());
        if (req.getStatus() != null) sponsor.setStatus(req.getStatus());
        return toSponsorResponse(sponsorRepo.save(sponsor));
    }

    @Transactional
    public void deleteSponsor(Long id) {
        sponsorRepo.deleteById(id);
    }

    private EventSponsorResponse toSponsorResponse(EventSponsor s) {
        // Prefer the enum field name, fall back to plain String tier
        String tierStr = s.getSponsorTier() != null ? s.getSponsorTier().name() : s.getTier();
        String statusStr = s.getSponsorStatus() != null ? s.getSponsorStatus().name() : s.getStatus();
        return EventSponsorResponse.builder()
                .id(s.getId())
                .eventId(s.getEvent().getId())
                .eventTitle(s.getEvent().getTitle())
                .name(s.getName())
                .tier(tierStr)
                .amountPledged(s.getAmountPledged())
                .amountReceived(s.getAmountReceived())
                .logoUrl(s.getLogoUrl())
                .contactName(s.getContactName())
                .contactPhone(s.getContactPhone())
                .contactEmail(s.getContactEmail())
                .status(statusStr)
                .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null)
                .build();
    }

    // ── Expenses ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventExpenseResponse> getExpenses(Long eventId) {
        List<EventExpense> expenses = eventId != null
                ? expenseRepo.findByEventIdOrderByCreatedAtDesc(eventId)
                : expenseRepo.findAll();
        return expenses.stream().map(this::toExpenseResponse).toList();
    }

    @Transactional
    public EventExpenseResponse createExpense(EventExpenseRequest req, AppUser currentUser) {
        EventCommunity event = findEvent(req.getEventId());
        EventExpense expense = EventExpense.builder()
                .event(event)
                .description(req.getDescription())
                .category(req.getCategory())
                .amount(req.getAmount())
                .vendorName(req.getVendorName())
                .receiptUrl(req.getReceiptUrl())
                .expenseDate(req.getExpenseDate() != null ? LocalDate.parse(req.getExpenseDate()) : null)
                .status(req.getStatus() != null ? req.getStatus() : "PENDING")
                .createdById(currentUser != null ? currentUser.getId() : null)
                .createdByName(currentUser != null ? currentUser.getFullName() : "Admin")
                .build();
        return toExpenseResponse(expenseRepo.save(expense));
    }

    @Transactional
    public EventExpenseResponse updateExpense(Long id, EventExpenseRequest req) {
        EventExpense expense = expenseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventExpense", id));
        expense.setDescription(req.getDescription());
        expense.setCategory(req.getCategory());
        expense.setAmount(req.getAmount());
        expense.setVendorName(req.getVendorName());
        expense.setReceiptUrl(req.getReceiptUrl());
        expense.setExpenseDate(req.getExpenseDate() != null ? LocalDate.parse(req.getExpenseDate()) : null);
        if (req.getStatus() != null) expense.setStatus(req.getStatus());
        return toExpenseResponse(expenseRepo.save(expense));
    }

    @Transactional
    public void deleteExpense(Long id) {
        expenseRepo.deleteById(id);
    }

    private EventExpenseResponse toExpenseResponse(EventExpense e) {
        return EventExpenseResponse.builder()
                .id(e.getId())
                .eventId(e.getEvent().getId())
                .eventTitle(e.getEvent().getTitle())
                .description(e.getDescription())
                .category(e.getCategory())
                .amount(e.getAmount())
                .vendorName(e.getVendorName())
                .receiptUrl(e.getReceiptUrl())
                .expenseDate(e.getExpenseDate() != null ? e.getExpenseDate().toString() : null)
                .status(e.getStatus())
                .createdByName(e.getCreatedByName())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }
}
