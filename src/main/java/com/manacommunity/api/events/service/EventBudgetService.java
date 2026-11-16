package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventBudgetRequest;
import com.manacommunity.api.events.dto.EventBudgetResponse;
import com.manacommunity.api.events.entity.EventBudget;
import com.manacommunity.api.events.entity.EventBudgetLineItem;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.repository.EventBudgetRepository;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventExpenseRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventBudgetService {

    private final EventBudgetRepository budgetRepository;
    private final EventCommunityRepository eventRepository;
    private final EventExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<EventBudgetResponse> listByCommunity(Long communityId) {
        return budgetRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream()
                .map(b -> toResponse(b, sumActuals(b.getEvent().getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventBudgetResponse getByEvent(Long eventId) {
        EventBudget budget = budgetRepository.findByEventIdWithLineItems(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("No budget found for event " + eventId));
        return toResponse(budget, sumActuals(eventId));
    }

    @Transactional
    public EventBudgetResponse createOrUpdate(Long eventId, EventBudgetRequest request) {
        EventCommunity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        EventBudget budget = budgetRepository.findByEventId(eventId)
                .orElseGet(() -> EventBudget.builder()
                        .event(event)
                        .communityId(request.getCommunityId())
                        .build());

        budget.setTotalPlannedAmount(request.getTotalPlannedAmount());
        budget.setNotes(request.getNotes());
        budget.getLineItems().clear();

        if (request.getLineItems() != null) {
            request.getLineItems().forEach(li -> {
                EventBudgetLineItem item = EventBudgetLineItem.builder()
                        .budget(budget)
                        .category(li.getCategory())
                        .description(li.getDescription())
                        .plannedAmount(li.getPlannedAmount())
                        .build();
                budget.getLineItems().add(item);
            });
        }

        EventBudget saved = budgetRepository.save(budget);
        return toResponse(saved, sumActuals(eventId));
    }

    @Transactional
    public void delete(Long eventId) {
        EventBudget budget = budgetRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("No budget found for event " + eventId));
        budgetRepository.delete(budget);
    }

    private BigDecimal sumActuals(Long eventId) {
        double raw = expenseRepository.sumAmountByEvent(eventId);
        return BigDecimal.valueOf(raw);
    }

    private EventBudgetResponse toResponse(EventBudget b, BigDecimal actual) {
        BigDecimal variance = b.getTotalPlannedAmount().subtract(actual);
        List<EventBudgetResponse.LineItemResponse> items = b.getLineItems().stream()
                .map(li -> EventBudgetResponse.LineItemResponse.builder()
                        .id(li.getId())
                        .category(li.getCategory())
                        .description(li.getDescription())
                        .plannedAmount(li.getPlannedAmount())
                        .createdAt(li.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return EventBudgetResponse.builder()
                .id(b.getId())
                .eventId(b.getEvent().getId())
                .eventTitle(b.getEvent().getTitle())
                .communityId(b.getCommunityId())
                .totalPlannedAmount(b.getTotalPlannedAmount())
                .totalActualAmount(actual)
                .variance(variance)
                .currency(b.getCurrency())
                .notes(b.getNotes())
                .lineItems(items)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
