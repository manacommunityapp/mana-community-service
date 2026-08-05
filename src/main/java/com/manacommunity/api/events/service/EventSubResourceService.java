package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventTaskRequest;
import com.manacommunity.api.events.dto.EventTaskResponse;
import com.manacommunity.api.events.dto.EventVolunteerRequest;
import com.manacommunity.api.events.dto.EventVolunteerResponse;
import com.manacommunity.api.events.dto.EventSponsorRequest;
import com.manacommunity.api.events.dto.EventSponsorResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventTask;
import com.manacommunity.api.events.entity.EventVolunteer;
import com.manacommunity.api.events.entity.EventSponsor;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventTaskRepository;
import com.manacommunity.api.events.repository.EventVolunteerRepository;
import com.manacommunity.api.events.repository.EventSponsorRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import com.manacommunity.api.events.dto.EventExpenseRequest;
import com.manacommunity.api.events.dto.EventExpenseResponse;
import com.manacommunity.api.events.entity.EventExpense;
import com.manacommunity.api.events.repository.EventExpenseRepository;
import com.manacommunity.api.user.model.AppUser;

@Service
@RequiredArgsConstructor
public class EventSubResourceService {

    private final CommunityEventRepository eventRepo;
    private final EventTaskRepository taskRepo;
    private final EventVolunteerRepository volunteerRepo;
    private final EventSponsorRepository sponsorRepo;
    private final EventExpenseRepository expenseRepo;

    // ── Helper ──────────────────────────────────────────────────────────────────
    private CommunityEvent findEvent(Long eventId) {
        return eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
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
        CommunityEvent event = findEvent(req.getEventId());
        EventTask task = EventTask.builder()
                .event(event)
                .title(req.getTitle())
                .description(req.getDescription())
                .phase(req.getPhase())
                .priority(req.getPriority() != null ? req.getPriority() : "MEDIUM")
                .assigneeName(req.getAssigneeName())
                .assigneeId(req.getAssigneeId())
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
        if (req.getPriority() != null) task.setPriority(req.getPriority());
        task.setAssigneeName(req.getAssigneeName());
        task.setAssigneeId(req.getAssigneeId());
        task.setDueDate(req.getDueDate());
        return toTaskResponse(taskRepo.save(task));
    }

    @Transactional
    public EventTaskResponse toggleDone(Long id) {
        EventTask task = taskRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventTask", id));
        task.setDone(!Boolean.TRUE.equals(task.getDone()));
        return toTaskResponse(taskRepo.save(task));
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepo.deleteById(id);
    }

    private EventTaskResponse toTaskResponse(EventTask t) {
        EventTaskResponse r = new EventTaskResponse();
        r.setId(t.getId());
        r.setEventId(t.getEvent().getId());
        r.setEventTitle(t.getEvent().getTitle());
        r.setTitle(t.getTitle());
        r.setDescription(t.getDescription());
        r.setPhase(t.getPhase());
        r.setPriority(t.getPriority());
        r.setAssigneeName(t.getAssigneeName());
        r.setAssigneeId(t.getAssigneeId());
        r.setDueDate(t.getDueDate() != null ? t.getDueDate().toString() : null);
        r.setDone(Boolean.TRUE.equals(t.getDone()));
        r.setCreatedAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
        return r;
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
        CommunityEvent event = findEvent(req.getEventId());
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
        vol.setStatus("CHECKED_IN");
        return toVolunteerResponse(volunteerRepo.save(vol));
    }

    @Transactional
    public EventVolunteerResponse checkOut(Long id) {
        EventVolunteer vol = volunteerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventVolunteer", id));
        vol.setCheckOutTime(LocalDateTime.now());
        vol.setStatus("CHECKED_OUT");
        return toVolunteerResponse(volunteerRepo.save(vol));
    }

    @Transactional
    public void deleteVolunteer(Long id) {
        volunteerRepo.deleteById(id);
    }

    private EventVolunteerResponse toVolunteerResponse(EventVolunteer v) {
        EventVolunteerResponse r = new EventVolunteerResponse();
        r.setId(v.getId());
        r.setEventId(v.getEvent().getId());
        r.setEventTitle(v.getEvent().getTitle());
        r.setUserId(v.getUserId());
        r.setUserName(v.getUserName());
        r.setRole(v.getRole());
        r.setZone(v.getZone());
        r.setShift(v.getShift());
        r.setStatus(v.getStatus());
        r.setCheckInTime(v.getCheckInTime() != null ? v.getCheckInTime().toString() : null);
        r.setCheckOutTime(v.getCheckOutTime() != null ? v.getCheckOutTime().toString() : null);
        r.setCreatedAt(v.getCreatedAt() != null ? v.getCreatedAt().toString() : null);
        return r;
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
        CommunityEvent event = findEvent(req.getEventId());
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
        EventSponsorResponse r = new EventSponsorResponse();
        r.setId(s.getId());
        r.setEventId(s.getEvent().getId());
        r.setEventTitle(s.getEvent().getTitle());
        r.setName(s.getName());
        r.setTier(s.getTier());
        r.setAmountPledged(s.getAmountPledged());
        r.setAmountReceived(s.getAmountReceived());
        r.setLogoUrl(s.getLogoUrl());
        r.setContactName(s.getContactName());
        r.setContactPhone(s.getContactPhone());
        r.setContactEmail(s.getContactEmail());
        r.setStatus(s.getStatus());
        r.setCreatedAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        return r;
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
        CommunityEvent event = findEvent(req.getEventId());
        EventExpense expense = EventExpense.builder()
                .event(event)
                .description(req.getDescription())
                .category(req.getCategory())
                .amount(req.getAmount())
                .vendorName(req.getVendorName())
                .receiptUrl(req.getReceiptUrl())
                .expenseDate(req.getExpenseDate())
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
        expense.setExpenseDate(req.getExpenseDate());
        if (req.getStatus() != null) expense.setStatus(req.getStatus());
        return toExpenseResponse(expenseRepo.save(expense));
    }

    @Transactional
    public void deleteExpense(Long id) {
        expenseRepo.deleteById(id);
    }

    private EventExpenseResponse toExpenseResponse(EventExpense e) {
        EventExpenseResponse r = new EventExpenseResponse();
        r.setId(e.getId());
        r.setEventId(e.getEvent().getId());
        r.setEventTitle(e.getEvent().getTitle());
        r.setDescription(e.getDescription());
        r.setCategory(e.getCategory());
        r.setAmount(e.getAmount());
        r.setVendorName(e.getVendorName());
        r.setReceiptUrl(e.getReceiptUrl());
        r.setExpenseDate(e.getExpenseDate() != null ? e.getExpenseDate().toString() : null);
        r.setStatus(e.getStatus());
        r.setCreatedByName(e.getCreatedByName());
        r.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        return r;
    }
}
