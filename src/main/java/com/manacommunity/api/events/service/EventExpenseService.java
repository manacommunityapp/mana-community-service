package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventExpenseRequest;
import com.manacommunity.api.events.dto.EventExpenseResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventExpense;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventExpenseRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventExpenseService {

    private final EventExpenseRepository expenseRepo;
    private final CommunityEventRepository eventRepo;

    @Transactional(readOnly = true)
    public List<EventExpenseResponse> getByEvent(Long eventId) {
        return expenseRepo.findByEventIdOrderByExpenseDateDesc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventExpenseResponse> getByCommunity(Long communityId) {
        return expenseRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventExpenseResponse create(EventExpenseRequest req, AppUser user, Community community) {
        CommunityEvent event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + req.getEventId()));

        EventExpense expense = EventExpense.builder()
                .event(event)
                .description(req.getDescription())
                .category(req.getCategory())
                .amount(req.getAmount())
                .vendorName(req.getVendorName())
                .receiptUrl(req.getReceiptUrl())
                .expenseDate(req.getExpenseDate() != null ? LocalDate.parse(req.getExpenseDate()) : null)
                .expenseStatus(parseEnumOrDefault(EventExpense.ExpenseStatus.class, req.getStatus(), EventExpense.ExpenseStatus.PENDING))
                .community(community)
                .createdBy(user)
                .build();
        return toResponse(expenseRepo.save(expense));
    }

    @Transactional
    public EventExpenseResponse update(Long id, EventExpenseRequest req) {
        EventExpense e = expenseRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
        e.setDescription(req.getDescription());
        e.setCategory(req.getCategory());
        e.setAmount(req.getAmount());
        e.setVendorName(req.getVendorName());
        e.setReceiptUrl(req.getReceiptUrl());
        e.setExpenseDate(req.getExpenseDate() != null ? LocalDate.parse(req.getExpenseDate()) : null);
        e.setExpenseStatus(parseEnumOrDefault(EventExpense.ExpenseStatus.class, req.getStatus(), e.getExpenseStatus() != null ? e.getExpenseStatus() : EventExpense.ExpenseStatus.PENDING));
        return toResponse(expenseRepo.save(e));
    }

    @Transactional
    public void delete(Long id) {
        expenseRepo.deleteById(id);
    }

    private EventExpenseResponse toResponse(EventExpense e) {
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
                .status(e.getExpenseStatus() != null ? e.getExpenseStatus().name() : e.getStatus())
                .createdByName(e.getCreatedBy() != null ? e.getCreatedBy().getFullName() : e.getCreatedByName())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        if (value == null || value.isBlank()) return def;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return def; }
    }
}
